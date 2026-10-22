package delver.vr;
import com.interrupt.dungeoneer.overlays.*;
public final class VrControlsRegression {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void reset(){for(Vr.Hand h:Vr.hands){h.button=h.oldButton=h.secondary=h.oldSecondary=h.stickClick=h.oldStickClick=false;h.valid=true;}}
    public static void run(){
        boolean[][] saved=new boolean[2][8];for(int i=0;i<2;i++){Vr.Hand h=Vr.hands[i];saved[i]=new boolean[]{h.valid,h.button,h.oldButton,h.secondary,h.oldSecondary,h.stickClick,h.oldStickClick,h.wasValid};}
        boolean map=VrSettings.mapVisible,bar=VrSettings.hotbarVisible,menu=Vr.current.getShowingMenu();
        try{
            if(menu)Vr.current.hideMenu();reset();
            Vr.hands[1].button=true;Vr.handleButtons();check(VrSettings.mapVisible!=map,"A did not toggle map state");check(OverlayManager.instance.current()==null&&!Vr.current.getShowingMenu(),"A opened a menu");
            Vr.hands[1].oldButton=true;Vr.handleButtons();check(VrSettings.mapVisible!=map,"Holding A repeated toggle");
            reset();Vr.hands[0].button=true;Vr.handleButtons();check(VrSettings.hotbarVisible!=bar,"X did not toggle hotbar state");check(OverlayManager.instance.current()==null,"X opened pause");
            reset();Vr.hands[0].secondary=true;Vr.handleButtons();check(Vr.current.getShowingMenu(),"Y did not open inventory");
            Vr.hands[0].oldSecondary=true;Vr.handleButtons();check(Vr.current.getShowingMenu(),"Holding Y closed inventory");
            reset();Vr.hands[0].secondary=true;Vr.handleButtons();check(!Vr.current.getShowingMenu()&&OverlayManager.instance.current()==null,"Y close opened pause");
            reset();Vr.hands[1].secondary=true;Vr.handleButtons();check(OverlayManager.instance.current() instanceof PauseOverlay,"B did not open pause");
            Vr.togglePause();OverlayManager.instance.draw(1f);reset();
            Vr.calibrated=false;Vr.hands[1].stickClick=true;Vr.handleButtons();check(Vr.calibrated,"Stick click did not recenter");
            com.badlogic.gdx.Gdx.app.log("DelverVR","VR CONTROLS PASS: A map, X hotbar, Y inventory open/close, B pause, right stick click recenter, edge handling");
        }finally{
            if(Vr.current.getShowingMenu()!=menu){if(menu)Vr.current.toggleInventory();else Vr.current.hideMenu();}
            VrSettings.mapVisible=map;VrSettings.hotbarVisible=bar;
            for(int i=0;i<2;i++){Vr.Hand h=Vr.hands[i];boolean[] v=saved[i];h.valid=v[0];h.button=v[1];h.oldButton=v[2];h.secondary=v[3];h.oldSecondary=v[4];h.stickClick=v[5];h.oldStickClick=v[6];h.wasValid=v[7];}
            NativeUi.update();
        }
    }
}
