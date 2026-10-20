package delver.vr;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;
import com.interrupt.dungeoneer.entities.*;
import com.interrupt.dungeoneer.entities.items.*;
import com.interrupt.dungeoneer.game.*;
import com.interrupt.dungeoneer.ui.*;
import java.lang.reflect.*;

/** Uses native bomb timers and the actual inventory Stage/ClickListener. */
public final class ItemUseRegression {
    public static final class BombProbe extends FusedBomb {
        int explosions;
        BombProbe(){wasSpawned=true;chanceIsLit=chanceIsDud=randomCountdownTimer=0;countdownTimer=timerStart=2;floating=true;isSolid=false;}
        public void explode(Level level){explosions++;isActive=false;}
        public void fizzle(Level level){}
    }
    public static final class FoodProbe extends Food {int uses;public boolean inventoryUse(Player p){uses++;return true;}}
    public static final class PotionProbe extends Potion {int uses;public boolean inventoryUse(Player p){uses++;return true;}}
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static int empty(Player p){int slot=p.inventory.indexOf(null,true);if(slot<0){p.addInventorySlot();slot=p.inventory.indexOf(null,true);}return slot;}
    static int put(Player p,Item item){int slot=empty(p);p.inventory.set(slot,item);return slot;}
    public static void run()throws Exception {
        Player p=Vr.current.player;Level level=Vr.current.level;Vr.Hand left=Vr.hands[0],right=Vr.hands[1];
        Array<Item> inventory=new Array<Item>(p.inventory);int inventorySize=p.inventorySize;
        Integer held=p.heldItem,selected=p.selectedBarItem;
        Item leftHeld=left.held,rightHeld=right.held;boolean leftValid=left.valid,rightValid=right.valid;
        Matrix4 leftWorld=new Matrix4(left.world),rightWorld=new Matrix4(right.world);
        Vector3 leftPos=new Vector3(left.pos),rightPos=new Vector3(right.pos),velocity=new Vector3(right.throwVelocity);
        Array<Entity> targets=Vr.interactionEntities;boolean menu=Vr.current.getShowingMenu();
        Array<Item> fixtures=new Array<Item>();
        try {
            if(menu)Vr.current.hideMenu();NativeUi.update();
            Vr.interactionEntities=new Array<Entity>();left.held=right.held=null;left.valid=right.valid=true;
            left.world.idt().translate(-100,100,-100);left.pos.set(-100,100,-100);
            right.pos.set(p.x,p.z+p.eyeHeight,p.y);right.world.idt().translate(right.pos);
            Method tick=Player.class.getDeclaredMethod("updatePlayerLight",Level.class,float.class);tick.setAccessible(true);
            BombProbe bomb=new BombProbe();fixtures.add(bomb);int slot=put(p,bomb);
            right.held=bomb;p.heldItem=p.selectedBarItem=slot;
            Vr.activate(1);check(bomb.isLit,"Right trigger did not light native fuse");
            tick.invoke(p,level,.5f);check(Math.abs(bomb.countdownTimer-1.5f)<.001f,"Held bomb ticked zero or two times");
            bomb.countdownTimer=.1f;Vr.activate(1);check(bomb.countdownTimer==.1f,"Repeated trigger extended a burning fuse");
            tick.invoke(p,level,1f);check(bomb.explosions==1&&right.held==null&&!p.inventory.contains(bomb,true),"Held bomb did not explode/leave inventory");
            BombProbe other=new BombProbe();fixtures.add(other);put(p,other);left.held=other;p.heldItem=null;
            Vr.activate(0);tick.invoke(p,level,3f);check(other.explosions==1&&left.held==null,"Unselected left-hand bomb fuse did not run");
            BombProbe thrown=new BombProbe();fixtures.add(thrown);put(p,thrown);right.held=thrown;
            right.throwVelocity.set(0,0,-5f*Vr.SCALE);Vr.release(1);
            check(thrown.isLit&&!p.inventory.contains(thrown,true),"Physical throw did not arm bomb");
            thrown.tick(level,3f);check(thrown.explosions==1,"Thrown bomb native timer did not explode");
            BombProbe placed=new BombProbe();fixtures.add(placed);put(p,placed);right.held=placed;right.throwVelocity.setZero();Vr.release(1);
            check(!placed.isLit,"Gently placing an unlit bomb armed it");
            BombProbe wet=new BombProbe();fixtures.add(wet);put(p,wet);right.held=wet;
            Field isWet=FusedBomb.class.getDeclaredField("isWet");isWet.setAccessible(true);isWet.setBoolean(wet,true);
            Vr.activate(1);check(!wet.isLit,"VR bypassed native wet-fuse behavior");right.held=null;
            BombProbe dud=new BombProbe();fixtures.add(dud);put(p,dud);dud.isDud=true;dud.chanceIsDud=1f;Vr.lightBomb(dud);dud.tick(level,3f);
            check(dud.explosions==0&&!dud.isLit,"VR bypassed native dud behavior");

            Vr.current.toggleInventory();NativeUi.update();p.ChangeHeldItem(null,false);
            check(NativeUi.active&&Vr.current.getShowingMenu(),"Inventory test did not open UI");
            FoodProbe food=new FoodProbe();PotionProbe potion=new PotionProbe();fixtures.add(food);fixtures.add(potion);
            int foodSlot=put(p,food),potionSlot=put(p,potion);Game.RefreshUI();
            p.selectedBarItem=p.heldItem=foodSlot;click(foodSlot,false);check(food.uses==1,"Selected food quick click did not consume");
            p.selectedBarItem=null;click(foodSlot,false);check(food.uses==2,"Unselected food quick click did not consume");
            p.selectedBarItem=p.heldItem=potionSlot;click(potionSlot,false);check(potion.uses==1,"Selected potion quick click did not consume");
            p.selectedBarItem=null;click(potionSlot,false);check(potion.uses==2,"Unselected potion quick click did not consume");
            click(foodSlot,true);check(food.uses==2,"Dragging food consumed it");
            com.badlogic.gdx.Gdx.app.log("DelverVR","ITEM USE PASS: bomb trigger/throw/timer/both hands/wet/dud; real inventory clicks selected/unselected food/potions; drag does not consume");
        }finally{
            NativeUi.down=false;
            for(Item item:fixtures){level.entities.removeValue(item,true);level.non_collidable_entities.removeValue(item,true);}
            p.inventory.clear();p.inventory.addAll(inventory);p.inventorySize=inventorySize;p.heldItem=held;p.selectedBarItem=selected;
            left.held=leftHeld;right.held=rightHeld;left.valid=leftValid;right.valid=rightValid;
            left.world.set(leftWorld);right.world.set(rightWorld);left.pos.set(leftPos);right.pos.set(rightPos);right.throwVelocity.set(velocity);Vr.interactionEntities=targets;
            if(menu&&!Vr.current.getShowingMenu())Vr.current.toggleInventory();else if(!menu&&Vr.current.getShowingMenu())Vr.current.hideMenu();
            Game.RefreshUI();NativeUi.update();
        }
    }
    static void click(int slot,boolean drag){
        Game game=Vr.current;Hotbar bar=slot<game.player.hotbarSize?Game.hudManager.quickSlots:Game.hudManager.backpack;
        bar.tickUI(game.input);MultiTouchButton button=bar.itemButtons.get(slot);check(button!=null,"Inventory button missing");
        Vector2 screen=Game.ui.stageToScreenCoordinates(button.localToStageCoordinates(new Vector2(button.getWidth()/2,button.getHeight()/2)));
        NativeUi.x=(int)screen.x;NativeUi.y=(int)screen.y;NativeUi.down=true;
        game.input.setCursorCatched(false);game.input.mouseMoved(NativeUi.x,NativeUi.y);game.input.touchDown(NativeUi.x,NativeUi.y,0,0);
        if(drag){NativeUi.x+=(int)(Game.GetUiSize()*1.5f);game.input.touchDragged(NativeUi.x,NativeUi.y,0);}
        bar.tickUI(game.input);NativeUi.down=false;game.input.touchUp(NativeUi.x,NativeUi.y,0,0);bar.tickUI(game.input);
    }
}
