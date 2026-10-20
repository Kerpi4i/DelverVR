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
    static int put(Player p,Item item){int slot=empty(p);p.inventory.set(slot,item);item.isActive=false;return slot;}
    static void onGround(Item item,Player p,Vr.Hand hand,Level level){
        item.x=hand.pos.x;item.y=hand.pos.z-.3f;item.z=hand.pos.y+.5f-item.collision.z*.5f;item.isActive=true;
        level.entities.add(item);Vr.interactionEntities.clear();Vr.interactionEntities.add(item);
    }
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
            BombProbe groundGrip=new BombProbe();fixtures.add(groundGrip);onGround(groundGrip,p,right,level);
            Vr.grab(1);
            check(right.held==groundGrip&&p.inventory.contains(groundGrip,true)&&!groundGrip.isActive,"Grip did not perform native ground-to-hand pickup");
            Vr.activate(1);check(groundGrip.isLit,"Upper trigger did not ignite held bomb");
            Method nativeTick=Player.class.getDeclaredMethod("updatePlayerLight",Level.class,float.class);nativeTick.setAccessible(true);
            nativeTick.invoke(p,level,.5f);
            check(groundGrip.isLit&&Math.abs(groundGrip.countdownTimer-1.5f)<.001f,"Ignition did not survive native equipped tick: lit="+groundGrip.isLit+" timer="+groundGrip.countdownTimer+" water="+groundGrip.inwater+" position="+groundGrip.x+","+groundGrip.y+","+groundGrip.z);
            groundGrip.countdownTimer=.1f;Vr.activate(1);
            check(groundGrip.countdownTimer==.1f,"Repeated trigger reset fuse");
            right.throwVelocity.set(0,0,-5f*Vr.SCALE);Vr.release(1);
            check(groundGrip.isActive&&groundGrip.isLit&&!p.inventory.contains(groundGrip,true),"Throw did not preserve lit bomb");
            groundGrip.tick(level,3f);check(groundGrip.explosions==1,"Native thrown fuse failed");
            for(int hand=0;hand<2;hand++)for(int kind=0;kind<7;kind++){
                Item item=kind==0?new BombProbe():kind==1?new FoodProbe():kind==2?new PotionProbe():kind==3?new Sword():kind==4?new Armor():kind==5?new Wand():new Scroll();
                fixtures.add(item);Vr.Hand h=Vr.hands[hand];
                h.pos.set(p.x,p.z+p.eyeHeight,p.y);h.world.idt().translate(h.pos);
                Vr.hands[1-hand].pos.set(-100,100,-100);Vr.hands[1-hand].world.idt().translate(-100,100,-100);
                h.held=null;p.ChangeHeldItem(null,false);onGround(item,p,h,level);
                Vr.activate(hand);
                check(p.inventory.contains(item,true)&&h.held==null&&p.GetHeldItem()==null,"Upper pickup failed "+item.getClass()+" hand "+hand);
                check(!(item instanceof FoodProbe)||((FoodProbe)item).uses==0,"Ground food consumed");
                check(!(item instanceof PotionProbe)||((PotionProbe)item).uses==0,"Ground potion consumed");
                p.removeFromInventory(item);level.entities.removeValue(item,true);onGround(item,p,h,level);
                Vr.grab(hand);
                check(p.inventory.contains(item,true)&&h.held==item,"Grip pickup failed "+item.getClass()+" hand "+hand);
                h.held=null;p.ChangeHeldItem(null,false);p.removeFromInventory(item);
            }
            BombProbe wet=new BombProbe();fixtures.add(wet);put(p,wet);right.held=wet;
            Field wetFlag=FusedBomb.class.getDeclaredField("isWet");wetFlag.setAccessible(true);wetFlag.setBoolean(wet,true);
            Vr.activate(1);check(!wet.isLit,"Wet bomb incorrectly ignited");right.held=null;
            Item occupied=new Sword();fixtures.add(occupied);int occupiedSlot=put(p,occupied);
            right.held=occupied;p.heldItem=p.selectedBarItem=occupiedSlot;
            right.pos.set(p.x,p.z+p.eyeHeight,p.y);right.world.idt().translate(right.pos);
            left.pos.set(-100,100,-100);left.world.idt().translate(left.pos);
            FoodProbe extra=new FoodProbe();fixtures.add(extra);onGround(extra,p,right,level);
            Vr.activate(1);
            check(p.inventory.contains(extra,true)&&right.held==occupied&&p.GetHeldItem()==occupied&&extra.uses==0,"Upper pickup with occupied hand changed equipment/consumed item");
            Vr.interactionEntities.clear();left.held=right.held=null;
            Vr.current.toggleInventory();NativeUi.update();p.ChangeHeldItem(null,false);
            check(NativeUi.active&&Vr.current.getShowingMenu(),"Inventory test did not open UI");
            FoodProbe food=new FoodProbe();PotionProbe potion=new PotionProbe();fixtures.add(food);fixtures.add(potion);
            int foodSlot=put(p,food),potionSlot=put(p,potion);Game.RefreshUI();
            p.selectedBarItem=p.heldItem=foodSlot;click(foodSlot,false);check(food.uses==1,"Selected food quick click did not consume");
            p.selectedBarItem=null;click(foodSlot,false);check(food.uses==2,"Unselected food quick click did not consume");
            p.selectedBarItem=p.heldItem=potionSlot;click(potionSlot,false);check(potion.uses==1,"Selected potion quick click did not consume");
            p.selectedBarItem=null;click(potionSlot,false);check(potion.uses==2,"Unselected potion quick click did not consume");
            click(foodSlot,true);check(food.uses==2,"Dragging food consumed it");
            com.badlogic.gdx.Gdx.app.log("DelverVR","ITEM USE PASS: native bomb throw/fuse, trigger ignition; seven ground item types, both hands, grip versus inventory pickup; inventory clicks and drag");
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
