package delver.vr;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.*;
import com.interrupt.dungeoneer.game.Game;
import com.interrupt.dungeoneer.gfx.GlRenderer;

/** The game's explored map texture, cropped around the player, on the right wrist. */
public final class WristMap {
    private FrameBuffer buffer;
    private com.badlogic.gdx.graphics.Texture arrow;
    private com.badlogic.gdx.graphics.glutils.ShaderProgram transparentMap;
    void prepare(GlRenderer r,Game g){
        if(g.level.mapIsDirty||g.level.dirtyMapTiles.size>0||r.mapTextureRegion==null)r.makeMapTextureForLevel(g.level);
        if(r.mapTextureRegion==null)return;
        if(buffer==null){buffer=new FrameBuffer(Pixmap.Format.RGBA8888,256,256,false);Pixmap p=new Pixmap(16,16,Pixmap.Format.RGBA8888);p.setColor(Color.WHITE);p.fillTriangle(8,0,1,15,8,11);p.fillTriangle(8,0,15,15,8,11);arrow=new com.badlogic.gdx.graphics.Texture(p);p.dispose();
            transparentMap=new com.badlogic.gdx.graphics.glutils.ShaderProgram("attribute vec4 a_position;attribute vec4 a_color;attribute vec2 a_texCoord0;uniform mat4 u_projTrans;varying vec4 v_color;varying vec2 v_texCoords;void main(){v_color=a_color;v_texCoords=a_texCoord0;gl_Position=u_projTrans*a_position;}",
                "#ifdef GL_ES\nprecision mediump float;\n#endif\nvarying vec4 v_color;varying vec2 v_texCoords;uniform sampler2D u_texture;void main(){vec4 c=texture2D(u_texture,v_texCoords);if(max(c.r,max(c.g,c.b))<0.035)discard;gl_FragColor=c*v_color;}");
            if(!transparentMap.isCompiled())throw new IllegalStateException(transparentMap.getLog());
        }
        com.badlogic.gdx.graphics.Texture map=r.mapTextureRegion.getTexture();
        TextureRegion region=new TextureRegion(map);
        float left=(g.player.x-9.5f)*4/map.getWidth(),top=(g.player.y-9.5f)*4/map.getHeight();
        region.setRegion(left,top,left+80f/map.getWidth(),top+80f/map.getHeight());
        buffer.begin();Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);Gdx.gl.glClearColor(0,0,0,0);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        SpriteBatch b=Vr.sprites;com.badlogic.gdx.graphics.glutils.ShaderProgram previous=b.getShader();b.setShader(transparentMap);b.setProjectionMatrix(new Matrix4().setToOrtho2D(0,0,256,256));b.begin();b.setColor(Color.WHITE);b.draw(region,6,6,244,244);b.end();b.setShader(previous);b.begin();
        b.setColor(.4f,.95f,.8f,1);b.draw(arrow,118,118,10,10,20,20,1,1,g.player.rot*MathUtils.radiansToDegrees+180,0,0,16,16,false,false);
        Vr.font.setColor(Color.WHITE);Vr.font.draw(b,"N",123,248);b.end();b.setColor(Color.WHITE);buffer.end();
    }
    void draw(){if(buffer==null||!Vr.hands[1].valid)return;
        float sleeve=HandRig.sleeveLength(WristHotbar.count(Vr.current.player));
        Matrix4 frame=new Matrix4(Vr.hands[1].world).translate(0,HandRig.TOP+.007f,HandRig.PALM_LENGTH+sleeve*.5f).rotate(Vector3.X,-90).rotate(Vector3.Z,-90);
        Decal d=Decal.newDecal(.19f*Vr.SCALE,.19f*Vr.SCALE,Vr.framebufferRegion(buffer.getColorBufferTexture()),true);
        d.setPosition(frame.getTranslation(new Vector3()));d.setRotation(frame.getRotation(new Quaternion(),true));Vr.wristDecals.add(d);
    }
    void dispose(){if(buffer!=null)buffer.dispose();if(arrow!=null)arrow.dispose();if(transparentMap!=null)transparentMap.dispose();}
}
