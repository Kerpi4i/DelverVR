package delver.vr;
import com.badlogic.gdx.math.*;
import com.interrupt.dungeoneer.entities.*;
import com.interrupt.dungeoneer.game.Level;
import com.interrupt.dungeoneer.tiles.Tile;
/** Restore the non-item paths of native keyboard USE, without injecting OS keys. */
public final class NativeUse {
    static boolean activate(){
        Matrix4 pose=new Matrix4(Vr.origin).mul(Vr.head);
        Vector3 start=pose.getTranslation(new Vector3()),direction=new Vector3(0,0,-1).rot(pose).nor();
        Entity best=null;float closest=Float.MAX_VALUE;
        for(Entity e:Vr.interactionEntities){
            if(e==Vr.current.player||e instanceof Item||!e.isActive||!Targets.usable(e))continue;
            Vector3 hit=Targets.contact(Targets.bounds(Vr.current.level,e),start,direction,0,.7f);
            if(hit!=null&&Vr.clear(start,hit)&&start.dst2(hit)<closest){closest=start.dst2(hit);best=e;}
        }
        if(best!=null){best.use(Vr.current.player,direction.x,direction.z);return true;}
        return tile(Vr.current.level,Vr.current.player,direction);
    }
    static boolean tile(Level level,Player p,Vector3 direction){
        // Native Player.Use walks up to the first blocking tile at this exact range.
        for(int i=1;i<10;i++){
            float distance=i/6f*.95f;
            Tile hit=level.getTile((int)Math.floor(p.x+direction.x*distance),(int)Math.floor(p.y+direction.z*distance));
            if(hit==null||!hit.blockMotion)continue;
            try{if(hit.getClass().getMethod("use").getDeclaringClass()==Tile.class)return false;}
            catch(NoSuchMethodException e){throw new IllegalStateException(e);}
            hit.use();return true;
        }
        return false;
    }
}
