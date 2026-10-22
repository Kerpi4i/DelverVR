package delver.vr;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import com.interrupt.dungeoneer.gfx.GlRenderer;
import com.interrupt.dungeoneer.gfx.drawables.DrawableMesh;

/** Compare original sky geometry with the different GL states inherited by eyes. */
public final class SkyRegression {
    public static void run(){
        GlRenderer renderer=Vr.renderer;
        PerspectiveCamera old=renderer.camera;
        PerspectiveCamera camera=new PerspectiveCamera(95,256,256);
        camera.near=.025f;camera.far=100;camera.direction.set(0,.5f,-1).nor();camera.update();
        DrawableMesh sky=new DrawableMesh("meshes/sky.obj","sky.png");
        sky.scale=4;sky.z=.5f;sky.fullbrite=true;sky.update();
        FrameBuffer frame=new FrameBuffer(Pixmap.Format.RGBA8888,256,256,true);
        Pixmap first=null,second=null,reference=null;
        try{
            renderer.camera=camera;
            first=draw(renderer,sky,frame,false);
            second=draw(renderer,sky,frame,true);
            frame.begin();
            try{
                Gdx.gl.glDepthMask(true);Gdx.gl.glClearDepthf(1);Gdx.gl.glClearColor(0,0,0,1);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
                Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);
                // Native mesh draw with a clean depth buffer, bypassing the VR sky hook.
                sky.draw(camera,Color.WHITE,GlRenderer.modelShaderInfo,100,100,GlRenderer.fogColor,GlRenderer.time);
                reference=com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixmap(0,0,256,256);
            }finally{frame.end();}
            int colored=0;
            for(int y=0;y<256;y++)for(int x=0;x<256;x++){
                int a=first.getPixel(x,y),b=second.getPixel(x,y);
                InteractionRegression.check(a==b,"Sky depends on inherited eye depth state at "+x+","+y);
                InteractionRegression.check(a==reference.getPixel(x,y),"Sky differs from native depth-tested geometry at "+x+","+y);
                if((a>>>8)!=0)colored++;
            }
            InteractionRegression.check(colored>20,"Sky comparison rendered no visible geometry");
            Gdx.app.log("DelverVR","SKY PASS: both inherited eye states match native depth-tested sky pixels, including stale depth buffer");
        }finally{
            renderer.camera=old;if(first!=null)first.dispose();if(second!=null)second.dispose();if(reference!=null)reference.dispose();frame.dispose();
            Gdx.gl.glClearDepthf(1);Gdx.gl.glDepthMask(true);Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        }
    }
    private static Pixmap draw(GlRenderer renderer,DrawableMesh sky,FrameBuffer frame,boolean depth){
        frame.begin();
        try{
            Gdx.gl.glDepthMask(true);Gdx.gl.glClearDepthf(depth?0:1);
            Gdx.gl.glClearColor(0,0,0,1);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
            if(depth)Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);else Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glDisable(GL20.GL_CULL_FACE);Gdx.gl.glDisable(GL20.GL_BLEND);
            renderer.renderSkybox(sky);
            return com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixmap(0,0,256,256);
        }finally{frame.end();Gdx.gl.glClearDepthf(1);}
    }
}
