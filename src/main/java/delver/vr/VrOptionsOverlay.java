package delver.vr;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.interrupt.dungeoneer.overlays.*;
import com.interrupt.dungeoneer.ui.UiSkin;
/** Native WindowOverlay: works with the existing VR menu pointer and gamepad order. */
public final class VrOptionsOverlay extends WindowOverlay {
    private static final String[] MAP={"Always visible","While held","Toggle"};
    public VrOptionsOverlay(boolean dim,boolean background){dimScreen=dim;showBackground=background;animateBackground=false;}
    private TextButton button(String text){TextButton b=new TextButton(text,UiSkin.getSkin().get(TextButton.TextButtonStyle.class));addGamepadButtonOrder(b,null);return b;}
    public Table makeContent(){
        VrSettings.load();Table t=new Table();t.defaults().pad(5);
        t.add(new Label("VR",UiSkin.getSkin())).colspan(2);t.row();
        t.add(new Label("Hand models",UiSkin.getSkin()));
        final TextButton hands=button(VrSettings.handsOnly?"Hands only":"Full arms");hands.setName("vr-hands");
        hands.addListener(new ClickListener(){public void clicked(InputEvent e,float x,float y){VrSettings.handsOnly=!VrSettings.handsOnly;hands.setText(VrSettings.handsOnly?"Hands only":"Full arms");VrSettings.save();}});t.add(hands);t.row();
        t.add(new Label("Wrist map",UiSkin.getSkin()));final TextButton map=button(MAP[VrSettings.mapMode]);map.setName("vr-map");
        map.addListener(new ClickListener(){public void clicked(InputEvent e,float x,float y){VrSettings.mapMode=(VrSettings.mapMode+1)%3;VrSettings.mapVisible=false;map.setText(MAP[VrSettings.mapMode]);VrSettings.save();}});t.add(map);t.row();
        t.add(new Label("Wrist hotbar",UiSkin.getSkin()));final TextButton hotbar=button(MAP[VrSettings.hotbarMode]);hotbar.setName("vr-hotbar");
        hotbar.addListener(new ClickListener(){public void clicked(InputEvent e,float x,float y){VrSettings.hotbarMode=(VrSettings.hotbarMode+1)%3;VrSettings.hotbarVisible=false;hotbar.setText(MAP[VrSettings.hotbarMode]);VrSettings.save();}});t.add(hotbar);t.row();
        String[] controls={"A: map    X: hotbar (chosen visibility mode)","Y: inventory / close    B: pause menu","Right stick click: recenter    Right stick: turn",
            "In menus: right stick up/down scrolls","Left stick: move / climb ladders","Grip: take item / release to throw or store","Trigger: collect / use / ignite bomb / UI click","Hotbar: point or touch, then grip or trigger","Bow: draw with other hand, release trigger"};
        for(String line:controls){Label label=new Label(line,UiSkin.getSkin());label.setFontScale(.68f);t.add(label).colspan(2).pad(1);t.row();}
        TextButton back=button("Back");back.addListener(new ClickListener(){public void clicked(InputEvent e,float x,float y){back();}});t.add(back).colspan(2);
        return t;
    }
    public void back(){VrSettings.save();OverlayManager.instance.replaceCurrent(new OptionsOverlay(dimScreen,showBackground));}
    public static void add(final OptionsOverlay options,Table table){
        TextButton b=new TextButton("VR",UiSkin.getSkin().get(TextButton.TextButtonStyle.class));b.setName("vr-settings");
        b.addListener(new ClickListener(){public void clicked(InputEvent e,float x,float y){options.saveOptions();OverlayManager.instance.replaceCurrent(new VrOptionsOverlay(options.dimScreen,options.showBackground));}});
        table.row();table.add(b).padTop(4);table.pack();options.addGamepadButtonOrder(b,null);
    }
}
