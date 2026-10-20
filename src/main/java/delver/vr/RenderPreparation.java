package delver.vr;

import com.interrupt.dungeoneer.entities.Entity;
import com.interrupt.dungeoneer.gfx.drawables.DrawableMesh;
import java.util.IdentityHashMap;

/** Frame-local reuse of camera-independent mesh preparation, never draw lists. */
public final class RenderPreparation {
    private static final IdentityHashMap<DrawableMesh, Entity> prepared = new IdentityHashMap<DrawableMesh, Entity>();
    private static boolean active;
    public static int updates, reused;
    public static void begin() { prepared.clear(); updates=0; reused=0; active=true; }
    public static void end() { active=false; prepared.clear(); }
    public static void update(Entity entity) {
        if (!active || !(entity.drawable instanceof DrawableMesh) || ((DrawableMesh)entity.drawable).bakeLighting) { entity.updateDrawable(); return; }
        // Identity matters: pooled/replaced drawables must still be initialized.
        if (prepared.get((DrawableMesh)entity.drawable)==entity) { reused++; return; }
        entity.updateDrawable();
        updates++;
        if(entity.drawable instanceof DrawableMesh) prepared.put((DrawableMesh)entity.drawable, entity);
    }
}
