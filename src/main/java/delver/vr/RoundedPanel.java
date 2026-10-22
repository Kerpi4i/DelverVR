package delver.vr;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
/** Nine-patch corners retain their radius instead of stretching with a window. */
public final class RoundedPanel {
    private static Texture texture;private static NinePatch patch;
    static void draw(SpriteBatch batch,float x,float y,float w,float h,float r,float g,float b,float a){
        if(patch==null){Pixmap p=new Pixmap(32,32,Pixmap.Format.RGBA8888);p.setColor(Color.WHITE);p.fillRectangle(8,0,16,32);p.fillRectangle(0,8,32,16);for(int dx:new int[]{8,23})for(int dy:new int[]{8,23})p.fillCircle(dx,dy,8);texture=new Texture(p);texture.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);p.dispose();patch=new NinePatch(texture,9,9,9,9);}
        patch.getColor().set(r,g,b,a);patch.draw(batch,x,y,w,h);
    }
    static void dispose(){if(texture!=null)texture.dispose();texture=null;patch=null;}
}
