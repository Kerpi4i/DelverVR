package delver.vr;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;
import com.interrupt.dungeoneer.game.Game;
import com.interrupt.dungeoneer.ui.*;
import com.interrupt.dungeoneer.statuseffects.StatusEffect;
/** Keep native inventory coordinates and hit tests, with a tightly bounded scrim. */
public final class InventoryPanel {
    static final Rectangle bounds=new Rectangle();
    static int effectCount(){int n=0;if(Vr.current.player.statusEffects!=null)for(StatusEffect e:Vr.current.player.statusEffects)if(e.active)n++;return n;}
    static Rectangle measure(){
        float size=Game.GetUiSize(),w=Gdx.graphics.getWidth(),h=Gdx.graphics.getHeight();
        int columns=Math.max(1,Game.hudManager.backpack.columns);
        int slots=Math.max(0,Vr.current.player.inventorySize-Vr.current.player.hotbarSize);
        int rows=(slots+columns-1)/columns;
        float left=w*.5f-size*columns*.5f,right=w*.5f+size*(columns*.5f+.85f);
        float bottom=h-size*(rows+1.7f);
        left=Math.min(left,w*.5f-size*Vr.current.player.hotbarSize*.5f);right=Math.max(right,w*.5f+size*Vr.current.player.hotbarSize*.5f);
        for(EquipLoc e:Game.hud.equipLocations.values())if(e.visible){left=Math.min(left,w*.5f+(e.xOffset-.5f)*size);right=Math.max(right,w*.5f+(e.xOffset+.5f)*size);bottom=Math.min(bottom,h-size*(1+e.yOffset));}
        // Native inventory/character tabs sit just to the right of the grid.
        bottom-=effectCount()>0?44+effectCount()*22:0;
        float pad=12;left=Math.max(8,left-pad);right=Math.min(w-8,right+pad);bottom=Math.max(8,bottom-pad);
        return bounds.set(left,bottom,right-left,h-bottom);
    }
    static void background(){
        Rectangle b=measure();SpriteBatch batch=Vr.sprites;batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight()));batch.begin();batch.setColor(Color.WHITE);
        RoundedPanel.draw(batch,b.x,b.y,b.width,b.height,.23f,.28f,.31f,.65f);RoundedPanel.draw(batch,b.x+1.5f,b.y+1.5f,b.width-3,b.height-3,.025f,.032f,.043f,.82f);batch.end();
    }
    static String duration(float ticks){int seconds=Math.max(1,(int)Math.ceil(ticks/60f));return seconds/60+":"+String.format(java.util.Locale.ROOT,"%02d",seconds%60);}
    static void effects(){
        if(effectCount()==0)return;SpriteBatch b=Vr.sprites;BitmapFont f=Vr.font;float sx=f.getData().scaleX,sy=f.getData().scaleY;
        b.setProjectionMatrix(new Matrix4().setToOrtho2D(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight()));b.begin();f.getData().setScale(1.1f);float y=bounds.y+18+effectCount()*22;
        f.setColor(.72f,.77f,.81f,1);f.draw(b,"Active effects",bounds.x+16,y+19);
        for(StatusEffect e:Vr.current.player.statusEffects)if(e.active){f.setColor(Color.WHITE);f.draw(b,e.name+"  "+duration(e.timer),bounds.x+16,y);y-=22;}
        b.end();f.getData().setScale(sx,sy);f.setColor(Color.WHITE);
    }
}
