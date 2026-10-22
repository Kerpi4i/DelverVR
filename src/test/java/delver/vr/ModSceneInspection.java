package delver.vr;
import com.badlogic.gdx.files.FileHandle;
import com.interrupt.dungeoneer.game.Level;
import com.interrupt.dungeoneer.entities.*;
import com.interrupt.dungeoneer.entities.triggers.*;
import com.interrupt.dungeoneer.serializers.KryoSerializer;
public final class ModSceneInspection {
 static class WarpProbe extends TriggeredWarp {int fired;public void doTriggerEvent(String value){fired++;}}
 static class ClearLevel extends Level {public boolean isFree(float x,float y,float z,com.badlogic.gdx.math.Vector3 c,float step,boolean floating,com.interrupt.dungeoneer.collision.Collision hit){return true;}}
 static void checkWarp(TriggeredWarp original){
   Level old=Vr.current.level;Player p=Vr.current.player;float x=p.x,y=p.y,z=p.z;
   com.badlogic.gdx.math.Matrix4 head=new com.badlogic.gdx.math.Matrix4(Vr.head),origin=new com.badlogic.gdx.math.Matrix4(Vr.origin);
   com.badlogic.gdx.utils.Array<Entity> targets=Vr.interactionEntities;
   try{
     WarpProbe probe=new WarpProbe();probe.x=original.x;probe.y=original.y;probe.z=original.z;probe.collision.set(original.collision);probe.isActive=true;
     Vr.current.level=new ClearLevel();p.x=probe.x;p.y=probe.y+.55f;p.z=probe.z;
     Vr.origin.idt();Vr.head.idt().translate(p.x,p.z+p.eyeHeight,p.y);
     com.badlogic.gdx.math.Vector3 direction=new com.badlogic.gdx.math.Vector3(probe.x,probe.z-.5f+probe.collision.z*.5f,probe.y).sub(p.x,p.z+p.eyeHeight,p.y).nor();
     Vr.head.rotate(new com.badlogic.gdx.math.Quaternion().setFromCross(new com.badlogic.gdx.math.Vector3(0,0,-1),direction));
     Vr.interactionEntities=new com.badlogic.gdx.utils.Array<Entity>();Vr.interactionEntities.add(probe);
     if(!NativeUse.activate())throw new AssertionError("Mod warp not targeted: "+original.levelToLoad);
     probe.tick(Vr.current.level,1000);if(probe.fired!=1)throw new AssertionError("Native warp event not fired: "+probe.fired);
     System.out.println("MOD WARP USE PASS "+original.levelToLoad+" collision="+original.collision);
   }finally{Vr.current.level=old;p.x=x;p.y=y;p.z=z;Vr.head.set(head);Vr.origin.set(origin);Vr.interactionEntities=targets;}
 }

 public static void run(){
   String root=System.getProperty("delver.vr.inspectMods");if(root==null)return;
   for(String name:new String[]{"tutorial.bin","start.bin","start-cave-mod.bin","dung.bin"})try{
     Level l=KryoSerializer.loadLevel(new FileHandle(root+"/levels/"+name));
     l.initPrefabs(Level.Source.LEVEL_START);
     int lights=0;for(Entity e:Targets.all(l,null)){
       if(e instanceof TriggeredWarp){TriggeredWarp w=(TriggeredWarp)e;if(name.equals("start.bin"))checkWarp(w);System.out.println("MOD WARP "+name+" "+w.x+","+w.y+","+w.z+" collision="+w.collision+" to="+w.levelToLoad+" trigger="+w.triggerType+" active="+w.isActive);}
       if(e instanceof Light){Light light=(Light)e;lights++;System.out.println("MOD LIGHT "+name+" pos="+e.x+","+e.y+","+e.z+" range="+light.range+" on="+light.on);}
       if(e instanceof DynamicLight){DynamicLight d=(DynamicLight)e;lights++;System.out.println("MOD DYNAMIC "+name+" range="+d.range+" on="+d.on);}
     }System.out.println("MOD LEVEL "+name+" sources="+lights);
   }catch(Exception e){System.out.println("MOD INSPECT "+name+": "+e);}
 }
}
