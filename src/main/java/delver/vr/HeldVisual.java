package delver.vr;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.interrupt.dungeoneer.entities.Item;
import com.interrupt.dungeoneer.entities.items.*;
/** Preserve the source sprite aspect ratio, including distinct held-item artwork. */
public final class HeldVisual {
    public static Vector2 size(int width,int height,float longest){
        float scale=longest/Math.max(1,Math.max(width,height));
        return new Vector2(width*scale,height*scale);
    }
    static TextureRegion texture(Item item){return item.getHeldInventoryTextureRegion(0);}
    static Vector2 dimensions(Item item){TextureRegion t=texture(item);return size(t.getRegionWidth(),t.getRegionHeight(),item instanceof Weapon?.26f:.10f);}
    static boolean blade(Item item){return item instanceof Weapon&&!(item instanceof Bow);}
    static Matrix4 orientation(Matrix4 hand,Item item){
        Matrix4 pose=new Matrix4(hand);
        // Delver's held blade artwork runs from bottom-right grip to top-left tip.
        if(blade(item)){Vector2 size=dimensions(item);pose.rotate(Vector3.X,-90).rotate(Vector3.Z,-MathUtils.atan2(size.x,size.y)*MathUtils.radiansToDegrees);}
        // Bow limbs are vertical, with the string toward the forearm (+Z).
        if(item instanceof Bow)pose.rotate(Vector3.Y,-90);
        return pose;
    }
    static Vector3 center(Matrix4 hand,Item item){
        if(item instanceof Bow){Vector2 size=dimensions(item);return new Vector3(size.x*.32f/Vr.SCALE,0,0).rot(orientation(hand,item)).add(new Vector3(0,0,-.035f).mul(hand));}
        if(!blade(item))return new Vector3(0,0,-.14f).mul(hand);
        Vector2 size=dimensions(item);
        return new Vector3(-size.x*.25f/Vr.SCALE,size.y*.25f/Vr.SCALE,0).mul(orientation(hand,item));
    }
    static Vector3 tip(Matrix4 hand,Item item){
        if(item==null||!blade(item))return new Vector3(0,0,-.12f).mul(hand);
        Vector2 size=dimensions(item);
        return new Vector3(-size.x*.75f/Vr.SCALE,size.y*.75f/Vr.SCALE,0).mul(orientation(hand,item));
    }
}
