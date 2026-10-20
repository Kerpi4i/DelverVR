package delver.vr;

import com.interrupt.dungeoneer.entities.*;
import com.interrupt.dungeoneer.game.Level;
import java.lang.reflect.Field;

/** Reuse Delver's current player step limit and its entity-step eligibility rules. */
public final class Locomotion {
    private static final Field STEP_HEIGHT;
    static {
        try{STEP_HEIGHT=Player.class.getDeclaredField("stepHeight");STEP_HEIGHT.setAccessible(true);}
        catch(Exception e){throw new ExceptionInInitializerError(e);}
    }
    static float stepHeight(Player p){try{return STEP_HEIGHT.getFloat(p);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    // Native forward acceleration .05 * walkSpeed, ground drag .8, 60 ticks/s.
    static float speed(Player p){return 15f*p.getWalkSpeed();}
    static float climb(float x,float y,boolean floor){
        float velocity=.02f*y*stick(x,y);
        return floor?Math.max(0,velocity):velocity;
    }
    static float stick(float x,float y){
        float length=(float)Math.sqrt(x*x+y*y);
        return length<=.18f?0:Math.min(1,(length-.18f)/(.95f-.18f))/length;
    }
    static void move(Level level,Player p,float dx,float dy){
        // Small sweeps retain collisions when a room-scale sample spans more than one edge.
        int steps=Math.max(1,(int)Math.ceil(Math.max(Math.abs(dx),Math.abs(dy))/.04f));
        for(int i=0;i<steps;i++){axis(level,p,dx/steps,true);axis(level,p,dy/steps,false);}
    }
    private static void axis(Level level,Player p,float amount,boolean xAxis){
        if(amount==0)return;
        float x=p.x+(xAxis?amount:0),y=p.y+(xAxis?0:amount),height=stepHeight(p);
        if(p.isSolid&&!level.isFree(x,y,p.z,p.collision,height,p.floating,null))return;
        Entity obstacle=null;
        if(p.isSolid){obstacle=level.getHighestEntityCollision(x,y,p.z,p.collision,p);if(obstacle==null)obstacle=level.checkStandingRoomWithEntities(x,y,p.z,p.collision,p);}
        if(obstacle!=null){
            float top=obstacle.z+obstacle.collision.z;
            if(!(p.z>top-height&&obstacle.canStepUpOn&&level.collidesWorldOrEntities(x,y,top,p.collision,p))){obstacle.encroached(p);return;}
            p.stepUp(top-p.z);p.z=top;obstacle.steppedOn(p);
        }
        p.x=x;p.y=y;
        // Terrain floor height and gravity are resolved by the normal player tick
        // immediately after the VR input, preserving slopes and water step limits.
    }
}
