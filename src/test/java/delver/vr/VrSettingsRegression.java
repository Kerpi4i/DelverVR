package delver.vr;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.interrupt.dungeoneer.overlays.*;
public final class VrSettingsRegression {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void click(Actor actor){check(actor!=null,"Missing VR setting button");for(EventListener listener:actor.getListeners())if(listener instanceof ClickListener)((ClickListener)listener).clicked(new InputEvent(),0,0);}
    public static void run(){
        boolean hands=VrSettings.handsOnly,visible=VrSettings.mapVisible;int mode=VrSettings.mapMode,hotbarMode=VrSettings.hotbarMode;boolean hotbarVisible=VrSettings.hotbarVisible;
        com.badlogic.gdx.Preferences prefs=Gdx.app.getPreferences("delver-vr");java.util.Map<String,Object> saved=new java.util.HashMap<String,Object>();saved.putAll(prefs.get());
        try{
            Table nativeOptions=new OptionsOverlay(false,false).makeContent();check(nativeOptions.findActor("vr-settings")!=null,"Native options missing VR entry");
            VrOptionsOverlay overlay=new VrOptionsOverlay(false,false);Table content=overlay.makeContent();
            click(content.findActor("vr-hands"));check(VrSettings.handsOnly!=hands,"Hand model button did not change setting");
            check(prefs.getBoolean("handsOnly")==VrSettings.handsOnly,"Hand model preference not saved");
            for(int i=0;i<3;i++)click(content.findActor("vr-map"));check(VrSettings.mapMode==mode,"Map modes did not cycle");
            click(content.findActor("vr-hotbar"));
            check(VrSettings.visible(0,false,false),"Always mode hidden");
            check(!VrSettings.visible(1,false,true)&&VrSettings.visible(1,true,false),"Hold mode wrong");
            check(VrSettings.visible(2,false,true)&&!VrSettings.visible(2,true,false),"Toggle mode wrong");
            check("1:00".equals(InventoryPanel.duration(3600)),"Effect timer not converted from game ticks");
            Gdx.app.log("DelverVR","VR SETTINGS PASS: native entry, separate map/hotbar controls, preferences, always/hold/toggle modes, effect duration");
        }finally{VrSettings.handsOnly=hands;VrSettings.mapMode=mode;VrSettings.mapVisible=visible;VrSettings.hotbarMode=hotbarMode;VrSettings.hotbarVisible=hotbarVisible;prefs.clear();prefs.put(saved);prefs.flush();}
    }
}
