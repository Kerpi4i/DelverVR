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
    static final float CELL=.039f;
    static int columns(int count){return Math.max(1,count);}
    static int rows(int count){return (count+columns(count)-1)/columns(count);}
    static float height(int count){return CELL*(rows(count)+.25f);}
    private FrameBuffer texture;
    private com.badlogic.gdx.graphics.Texture heart;
    static int count(Player p){return Math.max(0,Math.min(p.hotbarSize,p.inventory.size));}
    static Matrix4 frame(Matrix4 left){
        int slots=Vr.current==null?5:count(Vr.current.player);
        return new Matrix4(left).translate(-.015f,HandRig.TOP+.018f,HandRig.PALM_LENGTH+.04f).rotate(Vector3.X,-90).rotate(Vector3.Z,90);
    }
    static Vector3 slotCenter(Matrix4 left,int count,int index){
        return new Vector3((index%columns(count)+.5f-columns(count)*.5f)*CELL,(rows(count)-.5f-index/columns(count))*CELL-height(count)*.5f,0).mul(frame(left));
    }
    static int hover(Matrix4 left,Vector3 right,int count){
        if(count<=0)return -1;
        Vector3 local=new Vector3(right).mul(frame(left).inv());
        if(Math.abs(local.z)>.065f)return -1;
        int col=(int)Math.floor((local.x+columns(count)*CELL*.5f)/CELL);
        int row=rows(count)-1-(int)Math.floor((local.y+height(count)*.5f)/CELL);
        int index=row*columns(count)+col;
        return col>=0&&col<columns(count)&&row>=0&&row<rows(count)&&index<count?index:-1;
    }

    static int aim(Matrix4 left,Matrix4 rightPose,Vector3 right,int count){
        int near=hover(left,right,count);if(near>=0)return near;
        Matrix4 inverse=frame(left).inv();Vector3 start=new Vector3(right).mul(inverse),dir=new Vector3(0,0,-1).rot(rightPose).rot(inverse).nor();
        float t=Math.abs(dir.z)<.001f?-1:-start.z/dir.z;if(t<0||t>.65f)return -1;
        return hover(left,new Vector3(start).mulAdd(dir,t).mul(frame(left)),count);
    }
    private void createHeart(){
        if(heart!=null)return;
        Pixmap p=new Pixmap(16,16,Pixmap.Format.RGBA8888);p.setColor(Color.WHITE);
        p.fillCircle(5,5,4);p.fillCircle(10,5,4);p.fillTriangle(1,6,14,6,8,15);
        heart=new com.badlogic.gdx.graphics.Texture(p);p.dispose();
    }
    void prepare(Player p){
        int n=count(p);if(n==0)return;
        int w=columns(n)*96,h=rows(n)*96+24;
        if(texture==null||texture.getWidth()!=w||texture.getHeight()!=h){if(texture!=null)texture.dispose();texture=new FrameBuffer(Pixmap.Format.RGBA8888,w,h,false);}
        createHeart();
        SpriteBatch b=Vr.sprites;BitmapFont font=Vr.font;
        float oldX=font.getData().scaleX,oldY=font.getData().scaleY;
        texture.begin();Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);Gdx.gl.glClearColor(0,0,0,0);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        b.setProjectionMatrix(new Matrix4().setToOrtho2D(0,0,w,h));b.begin();
        RoundedPanel.draw(b,0,0,w,h,.025f,.035f,.05f,.84f);
        int hover=Vr.hands[0].valid&&Vr.hands[1].valid?aim(Vr.hands[0].world,Vr.hands[1].world,Vr.hands[1].pos,n):-1;
        int selected=p.heldItem==null?-1:p.heldItem;
        if(Vr.hands[1].held!=null)selected=p.inventory.indexOf(Vr.hands[1].held,true);
        for(int i=0;i<n;i++){
            float x=(i%columns(n))*96,y=(rows(n)-1-i/columns(n))*96;
            boolean active=i==selected;
            RoundedPanel.draw(b,x+3,y+4,90,94,active?.75f:i==hover?.32f:.22f,active?.58f:i==hover?.65f:.27f,active?.27f:i==hover?.63f:.30f,.9f);
            RoundedPanel.draw(b,x+5,y+6,86,90,active?.17f:.065f,active?.14f:.085f,active?.09f:.105f,.96f);
            Item item=p.inventory.get(i);
            b.setColor(Color.WHITE);font.setColor(.67f,.73f,.79f,1);font.getData().setScale(.8f);font.draw(b,Integer.toString(i+1),x+9,y+91);
            if(item==null)continue;
            TextureRegion icon=item.getInventoryTextureRegion();if(icon!=null)b.draw(icon,x+19,y+25,58,58);
            String amount=item instanceof ItemStack?Integer.toString(((ItemStack)item).count):item instanceof Wand?Integer.toString(((Wand)item).charges):"";
            if(!amount.isEmpty()){font.getData().setScale(1);font.setColor(Color.WHITE);font.draw(b,amount,x+6,y+24,80,com.badlogic.gdx.utils.Align.right,false);}
        }
        // Ten hearts reflect Delver HP rather than introducing a hunger mechanic.
        float ratio=MathUtils.clamp((float)p.hp/Math.max(1,p.getMaxHp()),0,1);
        float heartSize=14;float heartX=(w-10*(heartSize+1))*.5f;
        for(int i=0;i<10;i++){
            b.setColor(.23f,.09f,.10f,1);b.draw(heart,heartX+i*(heartSize+1),h-21,heartSize,heartSize);
            float fill=MathUtils.clamp(ratio*10-i,0,1);
            if(fill>0){b.setColor(.92f,.22f,.25f,1);b.draw(heart,heartX+i*(heartSize+1),h-21,heartSize*fill,heartSize,0,0,fill,1);}
        }
        b.setColor(Color.WHITE);font.getData().setScale(.9f);font.setColor(.86f,.9f,.93f,1);
        b.end();b.setColor(Color.WHITE);font.setColor(Color.WHITE);font.getData().setScale(oldX,oldY);texture.end();
    }
    private static void rect(SpriteBatch b,float x,float y,float w,float h,float r,float g,float blue,float alpha){b.setColor(r,g,blue,alpha);b.draw(Vr.white,x,y,w,h);}
    void draw(Player p){
        int n=count(p);if(texture==null||n==0||!Vr.hands[0].valid)return;
        Matrix4 transform=frame(Vr.hands[0].world);
        Decal d=Decal.newDecal(columns(n)*CELL*Vr.SCALE,height(n)*Vr.SCALE,Vr.framebufferRegion(texture.getColorBufferTexture()),true);
        d.setPosition(new Vector3(0,0,0).mul(transform));d.setRotation(transform.getRotation(new Quaternion(),true));Vr.wristDecals.add(d);
    }
    void dispose(){if(texture!=null)texture.dispose();if(heart!=null)heart.dispose();}
}
