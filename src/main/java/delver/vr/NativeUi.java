package delver.vr;

import com.badlogic.gdx.*;
import com.badlogic.gdx.math.*;
import java.lang.reflect.*;
import com.interrupt.dungeoneer.GameApplication;
import com.interrupt.dungeoneer.overlays.OverlayManager;
import com.interrupt.dungeoneer.entities.Item;
import com.interrupt.dungeoneer.entities.Player;
import com.interrupt.dungeoneer.entities.items.*;

/** Feed the existing Delver UI the same mouse events and polling state as a real mouse. */
public final class NativeUi {
    static final Matrix4 plane=new Matrix4();
    static final Vector3 pointerPoint=new Vector3();
    static boolean active,down,inside,waitRelease,inventoryShown;
    static int x,y;
    private static float scrollWait;
    private static Input proxyInput;
    static void install(){
        if(proxyInput!=null){Gdx.input=proxyInput;return;}
        final Input original=Gdx.input;
        proxyInput=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},(proxy,method,args)->{
            if(active){
                switch(method.getName()){
                    case "getX":return x;
                    case "getY":return y;
                    case "getDeltaX":case "getDeltaY":return 0;
                    case "isButtonPressed":return (Integer)args[0]==Input.Buttons.LEFT&&down;
                    case "isTouched":return down;
                    case "justTouched":return !waitRelease&&inside&&Vr.hands[1].trigger&&!Vr.hands[1].oldTrigger;
                }
            }
            try{return method.invoke(original,args);}catch(InvocationTargetException e){throw e.getCause();}
        });
        Gdx.input=proxyInput;
    }
    static boolean needed(){
        GameApplication app=GameApplication.instance;
        return app!=null&&(app.getScreen()!=app.mainScreen||OverlayManager.instance.current()!=null||(Vr.current!=null&&Vr.current.getShowingMenu()));
    }
    static void update(){
        // LWJGL3 restores the window's raw Input at the start of every frame.
        install();
        boolean inventory=Vr.current!=null&&Vr.current.getShowingMenu();
        if(inventoryShown&&!inventory){cancelClick();com.interrupt.dungeoneer.game.Game.ignoreEscape=true;}
        inventoryShown=inventory;
        if(!Vr.hands[1].trigger&&!Vr.hands[1].grip)waitRelease=false;
        boolean next=needed();
        if(next&&!active){
            Vector3 forward=new Vector3(0,0,-1).rot(Vr.head);forward.y=0;if(forward.len2()<.01f)forward.set(0,0,-1);forward.nor();
            Vector3 center=Vr.head.getTranslation(new Vector3()).mulAdd(forward,2);
            plane.idt().translate(center).rotate(Vector3.Y,MathUtils.atan2(-forward.x,-forward.z)*MathUtils.radiansToDegrees);
        }
        InputProcessor input=Gdx.input.getInputProcessor();
        if(!next){if(down&&input!=null)input.touchUp(x,y,0,0);down=false;inside=false;active=false;return;}
        active=true;Vr.Hand h=Vr.hands[1];inside=false;
        if(h.valid&&Vr.tracked){
            Matrix4 inverse=new Matrix4(plane).inv();
            Vector3 start=h.local.getTranslation(new Vector3()).mul(inverse),direction=new Vector3(0,0,-1).rot(h.local).rot(inverse).nor();
            float t=Math.abs(direction.z)<.001f?-1:-start.z/direction.z;
            Vector3 point=new Vector3(start).mulAdd(direction,t);
            pointerPoint.set(point).mul(plane);
            inside=t>0&&Math.abs(point.x)<=1.2f&&Math.abs(point.y)<=.675f;
            if(inside){x=(int)((point.x+1.2f)/2.4f*Gdx.graphics.getWidth());y=(int)((.675f-point.y)/1.35f*Gdx.graphics.getHeight());}
        }
        scrollWait=Math.max(0,scrollWait-Vr.dt);
        if(input!=null&&inside&&Math.abs(h.stickY)>.6f&&scrollWait==0){input.scrolled(h.stickY>0?-1:1);scrollWait=.16f;}
        if(input!=null){
            if(inside){input.mouseMoved(x,y);if(down&&h.trigger)input.touchDragged(x,y,0);}
            if(!waitRelease&&inside&&h.trigger&&!h.oldTrigger){down=true;input.touchDown(x,y,0,0);}
            if(down&&(!h.valid||!h.trigger)){down=false;input.touchUp(x,y,0,0);}
        }
    }
    static void cancelClick(){down=false;inside=false;waitRelease=true;}
    public static boolean useConsumable(Player player,int slot){
        if(!active||!Vr.tracked||Vr.current==null||Vr.current.player!=player||!Vr.current.getShowingMenu())return false;
        if(slot<0||slot>=player.inventory.size)return false;
        Item item=player.inventory.get(slot);
        // Do not change equip/unequip behavior for weapons and armor. The native
        // ClickListener already rejects drags before invoking UseInventoryItem.
        if(item instanceof Food||item instanceof Potion||item instanceof Scroll)return item.inventoryUse(player);
        return false;
    }
}
