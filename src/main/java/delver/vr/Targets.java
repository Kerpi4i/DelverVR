package delver.vr;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.*;
import com.badlogic.gdx.utils.Array;
import com.interrupt.dungeoneer.entities.*;
import com.interrupt.dungeoneer.game.Level;
import java.lang.reflect.Field;
import java.util.*;

/** Delver's AABB already converts its physics Z offset to rendered Y-up space. */
public final class Targets {
    public static BoundingBox bounds(Level level,Entity entity) {
        BoundingBox b=level.getAABB(new BoundingBox(),entity);
        Vector3 c=b.getCenter(new Vector3()),s=b.getDimensions(new Vector3()).scl(.5f);
        s.x=Math.max(s.x,.045f);s.y=Math.max(s.y,.045f);s.z=Math.max(s.z,.045f);
        return b.set(new Vector3(c).sub(s),new Vector3(c).add(s));
    }
    public static Vector3 closest(BoundingBox b,Vector3 p) {
        return new Vector3(MathUtils.clamp(p.x,b.min.x,b.max.x),MathUtils.clamp(p.y,b.min.y,b.max.y),MathUtils.clamp(p.z,b.min.z,b.max.z));
    }
    public static Vector3 contact(BoundingBox b,Vector3 start,Vector3 direction,float near,float reach) {
        Vector3 nearest=closest(b,start);
        if(start.dst2(nearest)<=near*near)return nearest;
        BoundingBox aim=new BoundingBox(new Vector3(b.min).sub(.025f,.025f,.025f),new Vector3(b.max).add(.025f,.025f,.025f));
        Vector3 hit=new Vector3();
        return Intersector.intersectRayBounds(new Ray(start,direction),aim,hit)&&start.dst2(hit)<=reach*reach?hit:null;
    }
    /** Match the native stair picking sphere, while retaining its player distance limit. */
    public static Vector3 stairContact(Stairs stairs,Player player,Vector3 start,Vector3 direction) {
        float dx=player.x-stairs.x,dy=player.y-stairs.y;
        if(dx*dx+dy*dy>1.1f*1.1f)return null;
        Vector3 center=new Vector3(stairs.x,stairs.z-.5f,stairs.y);
        float radius=stairs.collision.x/1.5f+.6f;
        Vector3 hit=new Vector3();
        if(start.dst2(center)<=radius*radius)return new Vector3(start);
        return Intersector.intersectRaySphere(new Ray(start,direction),center,radius,hit)&&start.dst2(hit)<=6.25f?hit:null;
    }
    public static Array<Entity> all(Level level,Object renderer) {
        Array<Entity> result=new Array<Entity>();
        Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
        collect(level,result,seen);
        // Static doors, actors and props can live in rendered chunks instead of level.entities.
        if(renderer!=null)collect(field(renderer,"chunks"),result,seen);
        return result;
    }
    private static void collect(Object o,Array<Entity> out,Set<Object> seen) {
        if(o==null||!seen.add(o))return;
        if(o instanceof Entity){out.add((Entity)o);return;}
        if(o instanceof Array){for(Object e:(Array<?>)o)collect(e,out,seen);return;}
        for(String name:new String[]{"entities","static_entities","non_collidable_entities","overworldChunk","up","down"})collect(field(o,name),out,seen);
    }
    private static Object field(Object o,String name) {
        for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException ignored){}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        return null;
    }
    public static boolean usable(Entity e) {
        if(e instanceof com.interrupt.dungeoneer.entities.triggers.Trigger)
            return ((com.interrupt.dungeoneer.entities.triggers.Trigger)e).triggerType==com.interrupt.dungeoneer.entities.triggers.Trigger.TriggerType.USE;
        try{Class<?> owner=e.getClass().getMethod("use",Player.class,float.class,float.class).getDeclaringClass();
            return owner!=Entity.class&&(owner!=Actor.class||((Actor)e).getUseTrigger()!=null);}
        catch(NoSuchMethodException impossible){throw new IllegalStateException(impossible);}
    }
}
