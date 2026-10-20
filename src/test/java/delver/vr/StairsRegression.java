package delver.vr;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;
import com.interrupt.dungeoneer.entities.*;
import com.interrupt.dungeoneer.game.Level;
public final class StairsRegression {
    public static final class Probe extends Stairs {
        int uses;
        Probe(StairDirection direction){super(direction);}
        public void changeLevel(Level level){uses++;}
    }
    public static void run(){
        Level old=Vr.current.level;Player p=Vr.current.player;Vr.Hand h=Vr.hands[0];
        Vector3 pos=new Vector3(h.pos);Matrix4 world=new Matrix4(h.world);Item held=h.held;
        Array<Entity> entities=Vr.interactionEntities;
        InteractionRegression.StepLevel level=new InteractionRegression.StepLevel();
        try{
            Vr.current.level=level;h.held=null;
            for(Stairs.StairDirection direction:Stairs.StairDirection.values()){
                Probe stairs=new Probe(direction);stairs.x=p.x;stairs.y=p.y-.7f;stairs.z=p.z;stairs.isActive=true;
                if(direction==Stairs.StairDirection.up)level.up=stairs;else level.down=stairs;
                Vr.interactionEntities=Targets.all(level,null);
                InteractionRegression.check(Vr.interactionEntities.contains(stairs,true),"Missing level stair reference");
                h.pos.set(p.x,p.z+1f,p.y);
                Vector3 aim=new Vector3(stairs.x,stairs.z-.5f,stairs.y).sub(h.pos).nor();
                h.world.idt().rotate(Vector3.Z.cpy().scl(-1),aim);
                InteractionRegression.check(Vr.target(h,false)==stairs,"Floor stairs unreachable from standing hand");
                Vr.activate(0);InteractionRegression.check(stairs.uses==1,"Native stair use did not dispatch "+direction);
                stairs.y=p.y-1.2f;
                InteractionRegression.check(Vr.target(h,false)==null,"Stairs activated beyond native distance");
                stairs.y=p.y-.7f;level.rise=1;
                InteractionRegression.check(Vr.target(h,false)==null,"Stairs selected through wall");level.rise=0;
                stairs.isActive=false;InteractionRegression.check(Vr.target(h,false)==null,"Inactive stairs selected");
                level.up=null;level.down=null;
            }
            com.badlogic.gdx.Gdx.app.log("DelverVR","STAIRS PASS: up/down native use, floor aim, range, occlusion, inactive");
        }finally{Vr.current.level=old;h.pos.set(pos);h.world.set(world);h.held=held;Vr.interactionEntities=entities;}
    }
}
