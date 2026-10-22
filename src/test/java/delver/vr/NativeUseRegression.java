package delver.vr;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;
import com.interrupt.dungeoneer.entities.*;
import com.interrupt.dungeoneer.game.Level;
import com.interrupt.dungeoneer.tiles.Tile;
public final class NativeUseRegression {
    static final class UseTile extends Tile{int uses;public void use(){uses++;}}
    static final class UseLevel extends Level {
        float rise;Tile entry;int atX,atY;
        public boolean isFree(float x,float y,float z,Vector3 c,float step,boolean floating,com.interrupt.dungeoneer.collision.Collision hit){return rise<=step;}
        public Tile getTile(int x,int y){return x==atX&&y==atY?entry:null;}
    }
    static final class Container extends Entity{public Array<Entity> entities=new Array<Entity>();}
    public static void run(){
        Player p=Vr.current.player;UseLevel level=new UseLevel();UseTile portal=new UseTile();portal.blockMotion=true;
        level.atX=(int)Math.floor(p.x)+1;level.atY=(int)Math.floor(p.y);level.entry=portal;
        InteractionRegression.check(NativeUse.tile(level,p,new Vector3(1,0,0))&&portal.uses==1,"Native USE tile transition not dispatched");
        Tile wall=new Tile();wall.blockMotion=true;level.entry=wall;
        InteractionRegression.check(!NativeUse.tile(level,p,new Vector3(1,0,0)),"Ordinary wall was treated as a transition");
        level.entry=portal;level.atX+=3;
        InteractionRegression.check(!NativeUse.tile(level,p,new Vector3(1,0,0))&&portal.uses==1,"Distant transition activated");
        Level previous=Vr.current.level;Array<Entity> targets=Vr.interactionEntities;Matrix4 origin=new Matrix4(Vr.origin),head=new Matrix4(Vr.head);
        try{
            Vr.current.level=level;Vr.origin.idt();Vr.head.idt().translate(p.x,p.z+p.eyeHeight,p.y);
            InteractionRegression.TalkProbe trigger=new InteractionRegression.TalkProbe();trigger.collision.set(.2f,.2f,.5f);trigger.x=p.x;trigger.y=p.y-.4f;trigger.z=p.z+p.eyeHeight+.25f;trigger.isActive=true;
            level.entities=new Array<Entity>();Container group=new Container();group.entities.add(trigger);level.entities.add(group);
            Vr.interactionEntities=Targets.all(level,null);
            InteractionRegression.check(Vr.interactionEntities.contains(trigger,true),"Nested mod trigger was not discovered");
            InteractionRegression.check(NativeUse.activate()&&trigger.uses==1,"Gaze USE did not activate invisible mod trigger");
            level.rise=1;InteractionRegression.check(!NativeUse.activate()&&trigger.uses==1,"Gaze USE bypassed wall occlusion");
        }finally{Vr.current.level=previous;Vr.interactionEntities=targets;Vr.origin.set(origin);Vr.head.set(head);}
        com.badlogic.gdx.Gdx.app.log("DelverVR","NATIVE USE PASS: special tile transitions, ordinary walls/range, nested hidden triggers, gaze and occlusion");
    }
}
