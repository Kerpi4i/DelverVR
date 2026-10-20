package delver.vr;

import com.badlogic.gdx.graphics.*;
import java.util.Random;

/** Small, nearest-filtered pixel tiles in Delver's muted cloth/leather palette. */
public final class HandTextures {
    static Pixmap skin(){
        int[] tones={0xa77854ff,0xb98760ff,0xc49369ff,0xd0a078ff,0x996747ff};
        Pixmap p=new Pixmap(16,16,Pixmap.Format.RGBA8888);p.setColor(tones[1]);p.fill();
        Random random=new Random(7301);
        for(int i=0;i<34;i++){p.setColor(tones[random.nextInt(tones.length)]);p.fillRectangle(random.nextInt(15),random.nextInt(15),2,1+random.nextInt(2));}
        // Blocky knuckle highlights and short creases, rather than smooth noise.
        for(int x=2;x<15;x+=4){p.setColor(tones[3]);p.fillRectangle(x,3,2,1);p.setColor(tones[4]);p.drawPixel(x,6);p.drawPixel(x+1,10);}
        return p;
    }
    static Pixmap sleeve(){
        int[] tones={0x354a50ff,0x3e5960ff,0x48646aff,0x526d71ff,0x2b3d43ff};
        Pixmap p=new Pixmap(32,32,Pixmap.Format.RGBA8888);p.setColor(tones[1]);p.fill();
        Random random=new Random(7302);
        for(int i=0;i<110;i++){p.setColor(tones[random.nextInt(tones.length)]);p.fillRectangle(random.nextInt(31),random.nextInt(31),1+random.nextInt(3),1+random.nextInt(2));}
        return p;
    }
    static Pixmap cuff(){
        Pixmap p=new Pixmap(8,8,Pixmap.Format.RGBA8888);p.setColor(0x69543eff);p.fill();
        p.setColor(0x8c7050ff);p.fillRectangle(0,2,8,2);p.setColor(0xb29969ff);p.fillRectangle(1,4,2,1);p.fillRectangle(5,4,2,1);
        return p;
    }
    static Texture texture(Pixmap pixels){Texture t=new Texture(pixels);pixels.dispose();t.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);t.setWrap(Texture.TextureWrap.Repeat,Texture.TextureWrap.Repeat);return t;}
}
