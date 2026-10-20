package delver.vr;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.math.*;
import com.interrupt.dungeoneer.game.Options;
import com.interrupt.dungeoneer.gfx.GlRenderer;
import com.interrupt.dungeoneer.gfx.shaders.ShaderInfo;
import com.interrupt.managers.ShaderManager;

/** Native filters applied to eye textures, without the desktop framebuffer path. */
final class PostProcessing {
    FrameBuffer copy,filtered,ping;
    final FrameBuffer[] blur=new FrameBuffer[3];
    SpriteBatch batch;
    float adaptation;
    int passes;
    void init(int w,int h){
        if(copy!=null&&copy.getWidth()==w&&copy.getHeight()==h)return;
        dispose();batch=new SpriteBatch();batch.disableBlending();
        copy=buffer(w,h);filtered=buffer(w,h);ping=buffer(Math.max(1,w/4),Math.max(1,h/4));
        for(int i=0;i<3;i++)blur[i]=buffer(ping.getWidth(),ping.getHeight());
    }
    FrameBuffer buffer(int w,int h){return new FrameBuffer(Pixmap.Format.RGBA8888,w,h,false);}
    void draw(Texture source,FrameBuffer target,ShaderProgram shader){
        target.begin();Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);Gdx.gl.glDisable(GL20.GL_CULL_FACE);Gdx.gl.glDepthMask(false);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0,0,target.getWidth(),target.getHeight()));
        batch.setShader(shader);batch.begin();
        if(shader!=null){if(shader.hasUniform("u_resolution"))shader.setUniformf("u_resolution",source.getWidth(),source.getHeight());}
        // FBO texture coordinates are already bottom-up, including all blur passes.
        batch.draw(source,0,0,target.getWidth(),target.getHeight(),0,0,1,1);batch.end();target.end();
    }
    void apply(FrameBuffer eye,int eyeIndex){
        Options o=Options.instance;
        boolean filter=o.enablePostProcessing&&o.postProcessingQuality>0&&o.postProcessFilter!=null;
        boolean fxaa=o.fxaaEnabled&&GlRenderer.fxaaShader!=null;
        if(!filter&&!fxaa)return;
        init(eye.getWidth(),eye.getHeight());
        draw(eye.getColorBufferTexture(),copy,null);
        Texture source=copy.getColorBufferTexture();
        if(filter){
            ShaderInfo info=ShaderManager.getShaderManager().getCompiledShader(o.postProcessFilter);
            if(info!=null&&info.shader!=null){
                int[] kernels={12,8,2};
                for(int i=0;i<3;i++){
                    int count=Math.max(2,Math.round(kernels[i]*Math.min(3,o.postProcessingQuality)/3f));
                    if((count&1)!=0)count++;
                    Texture input=source;
                    for(int j=0;j<count;j++){
                        FrameBuffer target=(j&1)==0?ping:blur[i];
                        ShaderProgram s=GlRenderer.blurShader;
                        s.begin();s.setUniformf("u_direction",(j&1)==0?count-j-1:0,(j&1)!=0?count-j-1:0);s.end();
                        input.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);
                        draw(input,target,s);input=target.getColorBufferTexture();
                    }
                }
                if(eyeIndex==0&&Vr.current!=null){
                    Vector3 center=new Matrix4(Vr.origin).mul(Vr.head).getTranslation(new Vector3());
                    Color c=Vr.current.level.getLightColorAt(center.x,center.z,center.y,null,new Color());
                    adaptation=MathUtils.lerp(adaptation,(c.r+c.g+c.b)/2.75f,Math.min(1,1.66f*Vr.dt));
                }
                ShaderProgram s=info.shader;info.begin(3);
                for(int i=0;i<3;i++){blur[i].getColorBufferTexture().bind(i+1);if(s.hasUniform("u_texture"+(i+1)))s.setUniformi("u_texture"+(i+1),i+1);}
                if(s.hasUniform("u_eyeAdaptation"))s.setUniformf("u_eyeAdaptation",adaptation);
                if(s.hasUniform("u_time"))s.setUniformf("u_time",GlRenderer.time);
                source.bind(0);source.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
                draw(source,filtered,s);info.end();source=filtered.getColorBufferTexture();passes++;
            }
        }
        draw(source,eye,fxaa?GlRenderer.fxaaShader:null);
        Gdx.gl.glDepthMask(true);Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
    }
    void dispose(){
        if(batch!=null){batch.dispose();batch=null;}
        if(copy!=null){copy.dispose();filtered.dispose();ping.dispose();copy=null;}
        for(int i=0;i<3;i++)if(blur[i]!=null){blur[i].dispose();blur[i]=null;}
    }
}
