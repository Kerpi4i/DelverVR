package delver.vr;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.interrupt.dungeoneer.entities.*;
import com.interrupt.dungeoneer.game.*;
public final class InventoryExpansionRegression {
 static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
 public static void run(){
  Player p=Vr.current.player;Array<Item> saved=new Array<Item>(p.inventory);int size=p.inventorySize,hotbar=p.hotbarSize;Integer held=p.heldItem,selected=p.selectedBarItem;boolean menu=Vr.current.getShowingMenu();
  Array<com.interrupt.dungeoneer.statuseffects.StatusEffect> oldEffects=p.statusEffects;
  try{
   p.inventorySize=23;p.hotbarSize=5;p.inventory.clear();for(int i=0;i<23;i++)p.inventory.add(null);p.heldItem=p.selectedBarItem=null;
   if(!menu)Vr.pToggleInventory();Game.RefreshUI();
   Rectangle initial=new Rectangle(InventoryPanel.measure());int checks=0;
   do{verify(p);checks++;if(!p.canAddInventorySlot())break;p.addInventorySlot();}while(true);
   check(p.inventorySize-p.hotbarSize==36,"Native backpack maximum changed");
   do{verify(p);checks++;if(!p.canAddHotbarSlot())break;p.addHotbarSlot();}while(true);
   check(p.hotbarSize==10&&p.inventorySize==46,"Native total slot maximum changed");
   p.addInventorySlot();p.addHotbarSlot();check(p.inventorySize==46&&p.hotbarSize==10,"Native upgrade caps were exceeded");
   Rectangle maximum=new Rectangle(InventoryPanel.measure());check(maximum.width>initial.width&&maximum.height>initial.height,"Inventory panel did not grow with native upgrades");
   int combinations=0;
   for(int bar=5;bar<=10;bar++)for(int bag=18;bag<=36;bag++){
     p.hotbarSize=bar;p.inventorySize=bar+bag;p.inventory.clear();for(int i=0;i<p.inventorySize;i++)p.inventory.add(null);Game.RefreshUI();verify(p);combinations++;
   }
   p.statusEffects=new Array<com.interrupt.dungeoneer.statuseffects.StatusEffect>();
   for(int i=0;i<2;i++){com.interrupt.dungeoneer.statuseffects.StatusEffect effect=new com.interrupt.dungeoneer.statuseffects.StatusEffect();effect.name=i==0?"Positive effect":"Negative effect";effect.timer=1800;p.statusEffects.add(effect);}
   Rectangle withEffects=new Rectangle(InventoryPanel.measure());check(withEffects.y<maximum.y,"Effect list did not expand the panel");
   Vr.renderScene(Vr.renderer,Vr.current);
   Vr.nativeUiFrame.begin();com.badlogic.gdx.graphics.Pixmap pixels=com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixmap(0,0,Vr.nativeUiFrame.getWidth(),Vr.nativeUiFrame.getHeight());Vr.nativeUiFrame.end();
   com.badlogic.gdx.graphics.PixmapIO.writePNG(Gdx.files.local("../inventory-max-preview.png"),pixels);pixels.dispose();
   check(Gdx.gl.glGetError()==0,"Maximum inventory render failed");
   Gdx.app.log("DelverVR","INVENTORY EXPANSION PASS: backpack 18..36, hotbar 5..10, total 46, "+checks+" native upgrade states, "+combinations+" size combinations, effects, fitted panel and max-size render; cell="+Game.GetUiSize());
  }finally{p.statusEffects=oldEffects;p.inventorySize=size;p.hotbarSize=hotbar;p.inventory.clear();p.inventory.addAll(saved);p.heldItem=held;p.selectedBarItem=selected;if(!menu)Vr.pToggleInventory();Game.RefreshUI();NativeUi.update();}
 }
 static void verify(Player p){
  Rectangle b=new Rectangle(InventoryPanel.measure());float cell=Game.GetUiSize(),w=Gdx.graphics.getWidth(),h=Gdx.graphics.getHeight();
  int columns=Game.hudManager.backpack.columns,rows=(p.inventorySize-p.hotbarSize+columns-1)/columns;
  check(Game.hudManager.backpack.rows==rows&&Game.hudManager.quickSlots.columns==p.hotbarSize,"Native HUD grid dimensions do not match capacity");
  for(com.interrupt.dungeoneer.ui.EquipLoc e:Game.hud.equipLocations.values())if(e.visible){
    check(b.x<=w*.5f+(e.xOffset-.5f)*cell&&b.x+b.width>=w*.5f+(e.xOffset+.5f)*cell&&b.y<=h-cell*(1+e.yOffset),"Equipment slot outside expanded panel");
  }
  check(b.x<=w*.5f-cell*p.hotbarSize*.5f&&b.x+b.width>=w*.5f+cell*p.hotbarSize*.5f,"Expanded hotbar outside panel");
  check(b.y<=h-cell*(rows+1.7f)&&b.y+b.height>=h,"Expanded backpack outside panel");
  check(b.x>=0&&b.y>=0&&b.x+b.width<=w&&b.y+b.height<=h,"Panel outside capture surface");
  for(int i=0;i<p.hotbarSize;i++)check(WristHotbar.hover(Vr.hands[0].world,WristHotbar.slotCenter(Vr.hands[0].world,p.hotbarSize,i),p.hotbarSize)==i,"Expanded wrist slot unreachable: "+i);
 }
}
