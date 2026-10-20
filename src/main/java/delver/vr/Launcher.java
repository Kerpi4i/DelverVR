package delver.vr;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.interrupt.dungeoneer.GameApplication;
import com.interrupt.dungeoneer.game.Options;

public final class Launcher implements ApplicationListener {
    private int frames;
    private final GameApplication game = new GameApplication();
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration c = new Lwjgl3ApplicationConfiguration();
        c.setTitle("Delver VR — Quest / SteamVR");
        c.setWindowedMode(1280, 720);
        c.useVsync(false);
        c.setAudioConfig(32,512,18);
        new Lwjgl3Application(new Launcher(), c);
    }
    public void create() {
        Options.loadOptions();
        Options.instance.headBobEnabled=false;
        game.create();
        Vr.init();
    }
    public void render() {
        if(Boolean.getBoolean("delver.vr.smoke")&&++frames==30){GameApplication.ShowMainScreen();com.interrupt.dungeoneer.GameManager.getGame().progression.sawTutorial=true;}
        Vr.begin();
        if(Boolean.getBoolean("delver.vr.smoke"))while(Gdx.gl.glGetError()!=0){}
        game.render();
        if(Boolean.getBoolean("delver.vr.smoke")&&frames==10){
            // Asymmetric labels exercise the same capture -> VR panel -> mirror
            // path as the game menus, detecting both horizontal and vertical flips.
            Vr.sprites.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight()));
            Vr.sprites.begin();Vr.font.setColor(com.badlogic.gdx.graphics.Color.WHITE);
            Vr.font.draw(Vr.sprites,"TOP LEFT  ->  RIGHT",32,Gdx.graphics.getHeight()-32);
            Vr.font.draw(Vr.sprites,"BOTTOM LEFT",32,48);Vr.sprites.end();
        }
        if(Boolean.getBoolean("delver.vr.smoke")){int e=Gdx.gl.glGetError();if(e!=0)Gdx.app.log("DelverVR","Game render GL error="+e+" frame="+frames);}
        Vr.finish();
        if(Boolean.getBoolean("delver.vr.regression")&&frames==50)try{
            Class.forName("delver.vr.InteractionRegression").getMethod("run").invoke(null);
            Class.forName("delver.vr.LadderRegression").getMethod("run").invoke(null);
            Class.forName("delver.vr.ItemUseRegression").getMethod("run").invoke(null);
            Class.forName("delver.vr.ShadowBatchRegression").getMethod("run").invoke(null);
        }catch(Exception e){throw new RuntimeException("VR interaction regression failed",e);}
        if(Boolean.getBoolean("delver.vr.smoke.save")&&frames==31)com.interrupt.dungeoneer.GameManager.getGame().save();
        if(Boolean.getBoolean("delver.vr.smoke")&&frames==60)Vr.current.toggleInventory();
        if(Boolean.getBoolean("delver.vr.smoke")&&(frames==10||frames==32||frames==100)){
            int w=Gdx.graphics.getWidth(),h=Gdx.graphics.getHeight();
            com.badlogic.gdx.graphics.Pixmap image=new com.badlogic.gdx.graphics.Pixmap(w,h,com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            image.getPixels().put(com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixels(0,0,w,h,true)).flip();
            com.badlogic.gdx.graphics.PixmapIO.writePNG(Gdx.files.local("../smoke-"+frames+".png"),image);image.dispose();
        }
        if(Boolean.getBoolean("delver.vr.smoke")&&frames==100&&Vr.current!=null)Gdx.app.log("DelverVR","camera="+Vr.activeCamera.position+" direction="+Vr.activeCamera.direction+" playerZ="+Vr.current.player.z+" level="+Vr.current.level.levelFileName);
        if(Boolean.getBoolean("delver.vr.smoke")&&frames>=180){Gdx.app.log("DelverVR","SMOKE PASS: 180 frames, GL error="+Gdx.gl.glGetError());Gdx.app.exit();}
    }
    public void resize(int w,int h) { game.resize(w,h); }
    public void pause() { game.pause(); }
    public void resume() { game.resume(); }
    public void dispose() {
        try {game.dispose();} finally {
            com.interrupt.dungeoneer.game.Game.threadPool.shutdown();
            Vr.dispose();
        }
    }
}

