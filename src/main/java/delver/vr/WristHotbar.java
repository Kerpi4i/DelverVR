package delver.vr;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.*;
import com.interrupt.dungeoneer.entities.*;
import com.interrupt.dungeoneer.entities.items.*;

/** One row of real quick slots, attached to the top of the left forearm. */
public final class WristHotbar {
    static final float CELL=.055f, HEIGHT=CELL*152f/96f;
    private FrameBuffer texture;
    private com.badlogic.gdx.graphics.Texture heart;
    static int count(Player p){return Math.max(0,Math.min(p.hotbarSize,p.inventory.size));}
    static Matrix4 frame(Matrix4 left){
        int slots=Vr.current==null?5:count(Vr.current.player);
        return new Matrix4(left).translate(0,HandRig.TOP+.007f,HandRig.hotbarCenter(slots)).rotate(Vector3.X,-90).rotate(Vector3.Z,90);
    }
    static Vector3 slotCenter(Matrix4 left,int count,int index){
        return new Vector3((index+.5f-count*.5f)*CELL,-HEIGHT*.158f,0).mul(frame(left));
    }
    static int hover(Matrix4 left,Vector3 right,int count){
        if(count<=0)return -1;
        Vector3 local=new Vector3(right).mul(frame(left).inv());
        if(Math.abs(local.z)>.065f||local.y<-HEIGHT*.5f||local.y>HEIGHT*.16f)return -1;
        int index=(int)Math.floor((local.x+count*CELL*.5f)/CELL);
        return index>=0&&index<count?index:-1;
    }
    static boolean inventoryButton(Matrix4 left,Vector3 right,int count){
        Vector3 local=new Vector3(right).mul(frame(left).inv());
        return Math.abs(local.z)<.065f&&local.y>=-HEIGHT*.5f&&local.y<=HEIGHT*.5f&&local.x>=count*CELL*.5f&&local.x<(count*.5f+1)*CELL;
    }
    private void createHeart(){
        if(heart!=null)return;
        Pixmap p=new Pixmap(16,16,Pixmap.Format.RGBA8888);p.setColor(Color.WHITE);
        p.fillCircle(5,5,4);p.fillCircle(10,5,4);p.fillTriangle(1,6,14,6,8,15);
        heart=new com.badlogic.gdx.graphics.Texture(p);p.dispose();
    }
    void prepare(Player p){
        int n=count(p);if(n==0)return;
        int w=(n+1)*96;
        if(texture==null||texture.getWidth()!=w){if(texture!=null)texture.dispose();texture=new FrameBuffer(Pixmap.Format.RGBA8888,w,152,false);}
        createHeart();
        SpriteBatch b=Vr.sprites;BitmapFont font=Vr.font;
        float oldX=font.getData().scaleX,oldY=font.getData().scaleY;
        texture.begin();Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);Gdx.gl.glClearColor(0,0,0,0);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        b.setProjectionMatrix(new Matrix4().setToOrtho2D(0,0,w,152));b.begin();
        rect(b,0,0,w,152,.025f,.035f,.05f,.88f);
        int hover=Vr.hands[0].valid&&Vr.hands[1].valid?hover(Vr.hands[0].world,Vr.hands[1].pos,n):-1;
        int selected=p.heldItem==null?-1:p.heldItem;
        if(Vr.hands[1].held!=null)selected=p.inventory.indexOf(Vr.hands[1].held,true);
        for(int i=0;i<n;i++){
            float x=i*96;
            boolean active=i==selected;
            rect(b,x+3,4,90,94,active?.34f:.10f,active?.27f:.14f,active?.12f:.19f,.95f);
            if(active)rect(b,x+3,4,90,3,.95f,.72f,.27f,1);
            if(i==hover)rect(b,x+3,95,90,3,.35f,.9f,.85f,1);
            Item item=p.inventory.get(i);
            b.setColor(Color.WHITE);font.setColor(.67f,.73f,.79f,1);font.getData().setScale(.8f);font.draw(b,Integer.toString(i+1),x+9,91);
            if(item==null)continue;
            TextureRegion icon=item.getInventoryTextureRegion();if(icon!=null)b.draw(icon,x+19,25,58,58);
            String amount=item instanceof ItemStack?Integer.toString(((ItemStack)item).count):item instanceof Wand?Integer.toString(((Wand)item).charges):"";
            if(!amount.isEmpty()){font.getData().setScale(1);font.setColor(Color.WHITE);font.draw(b,amount,x+6,24,80,com.badlogic.gdx.utils.Align.right,false);}
        }
        boolean buttonHover=Vr.hands[0].valid&&Vr.hands[1].valid&&inventoryButton(Vr.hands[0].world,Vr.hands[1].pos,n);
        rect(b,n*96+5,5,86,93,buttonHover?.2f:.1f,buttonHover?.4f:.22f,buttonHover?.45f:.27f,1);
        b.setColor(Color.WHITE);font.setColor(Color.WHITE);font.getData().setScale(1.1f);
        font.draw(b,"BAG",n*96+8,59,80,com.badlogic.gdx.utils.Align.center,false);
        // Ten hearts reflect Delver HP rather than introducing a hunger mechanic.
        float ratio=MathUtils.clamp((float)p.hp/Math.max(1,p.getMaxHp()),0,1);
        float heartSize=Math.min(19,(w*.48f-10)/10f);
        for(int i=0;i<10;i++){
            b.setColor(.23f,.09f,.10f,1);b.draw(heart,9+i*(heartSize+1),123,heartSize,heartSize);
            float fill=MathUtils.clamp(ratio*10-i,0,1);
            if(fill>0){b.setColor(.92f,.22f,.25f,1);b.draw(heart,9+i*(heartSize+1),123,heartSize*fill,heartSize,0,0,fill,1);}
        }
        b.setColor(Color.WHITE);font.getData().setScale(.9f);font.setColor(.86f,.9f,.93f,1);
        font.draw(b,p.hp+" / "+p.getMaxHp(),9,117);
        font.setColor(.65f,.85f,.77f,1);font.draw(b,"LV "+p.level,w*.58f,139);
        font.setColor(.95f,.76f,.34f,1);font.draw(b,"G "+p.gold,w*.58f,117);
        font.setColor(.7f,.78f,.87f,1);font.draw(b,"ARM "+p.GetArmorClass(),w*.78f,139);
        b.end();b.setColor(Color.WHITE);font.setColor(Color.WHITE);font.getData().setScale(oldX,oldY);texture.end();
    }
    private static void rect(SpriteBatch b,float x,float y,float w,float h,float r,float g,float blue,float alpha){b.setColor(r,g,blue,alpha);b.draw(Vr.white,x,y,w,h);}
    void draw(Player p){
        int n=count(p);if(texture==null||n==0||!Vr.hands[0].valid)return;
        Matrix4 transform=frame(Vr.hands[0].world);
        Decal d=Decal.newDecal((n+1)*CELL*Vr.SCALE,HEIGHT*Vr.SCALE,Vr.framebufferRegion(texture.getColorBufferTexture()),true);
        d.setPosition(new Vector3(CELL*.5f,0,0).mul(transform));d.setRotation(transform.getRotation(new Quaternion(),true));Vr.wristDecals.add(d);
    }
    void dispose(){if(texture!=null)texture.dispose();if(heart!=null)heart.dispose();}
}
