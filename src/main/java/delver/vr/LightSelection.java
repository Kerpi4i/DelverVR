package delver.vr;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.interrupt.dungeoneer.gfx.DynamicLight;
import java.util.Comparator;
/** Rank the native four-light budget by the same attenuation used by the shader. */
public final class LightSelection {
    private static final Vector3 position=new Vector3();
    private static final Comparator<DynamicLight> order=new Comparator<DynamicLight>(){public int compare(DynamicLight a,DynamicLight b){int c=Float.compare(score(b,position),score(a,position));return c!=0?c:Float.compare(a.position.dst2(position),b.position.dst2(position));}};
    static float score(DynamicLight light,Vector3 point){
        float brightness=Math.max(Math.abs(light.color.x),Math.max(Math.abs(light.color.y),Math.abs(light.color.z))),radius=light.range*.3333f;
        if(radius<=0||brightness<=0)return 0;
        return brightness/(1+light.position.dst2(point)/(radius*radius));
    }
    public static void sort(Array<DynamicLight> lights,Vector3 center){position.set(center);Vr.lightSortPosition(position);lights.sort(order);}
}
