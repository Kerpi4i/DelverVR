package delver.vr;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
public final class VrSettings {
    public static boolean handsOnly=false,mapVisible=false,hotbarVisible=false;
    public static int mapMode=1,hotbarMode=1;
    private static boolean loaded;
    static int mode(int n){return Math.max(0,Math.min(2,n));}
    static void load(){if(loaded)return;Preferences p=Gdx.app.getPreferences("delver-vr");handsOnly=p.getBoolean("handsOnly",false);mapMode=mode(p.getInteger("wristMapMode",1));hotbarMode=mode(p.getInteger("hotbarMode",1));loaded=true;}
    static void save(){Preferences p=Gdx.app.getPreferences("delver-vr");p.putBoolean("handsOnly",handsOnly);p.putInteger("wristMapMode",mapMode);p.putInteger("hotbarMode",hotbarMode);p.flush();}
    static boolean visible(int mode,boolean down,boolean toggled){return mode==0||(mode==1?down:toggled);}
    static boolean showMap(){return Vr.hands[1].valid&&visible(mapMode,Vr.hands[1].button,mapVisible);}
    static boolean showHotbar(){return Vr.hands[0].valid&&visible(hotbarMode,Vr.hands[0].button,hotbarVisible);}
    static void buttons(){if(Vr.hands[1].valid&&Vr.hands[1].button&&!Vr.hands[1].oldButton)mapVisible=!mapVisible;if(Vr.hands[0].valid&&Vr.hands[0].button&&!Vr.hands[0].oldButton)hotbarVisible=!hotbarVisible;}
}
