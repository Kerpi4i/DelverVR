package delver.vr;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

/** Solid low-poly hands, with no wireframe or collision-box visualization. */
public final class BlockHands {
    private Model skin,sleeve,cuff;
    private Texture skinTexture,sleeveTexture,cuffTexture;
    private float builtSleeveLength=-1;
    private ModelBatch batch;
    private Environment light;
    private final ModelInstance[] palms=new ModelInstance[2],arms=new ModelInstance[2],cuffs=new ModelInstance[2];
    private void init(){
        if(batch!=null)return;
        skinTexture=HandTextures.texture(HandTextures.skin());sleeveTexture=HandTextures.texture(HandTextures.sleeve());
        cuffTexture=HandTextures.texture(HandTextures.cuff());
        skin=PixelBox.create(.085f,.07f,.15f,new Material(ColorAttribute.createDiffuse(Color.WHITE),TextureAttribute.createDiffuse(skinTexture)),16);
        cuff=PixelBox.create(.098f,.088f,.018f,new Material(ColorAttribute.createDiffuse(Color.WHITE),TextureAttribute.createDiffuse(cuffTexture)),8);
        for(int i=0;i<2;i++){palms[i]=new ModelInstance(skin);cuffs[i]=new ModelInstance(cuff);}
        batch=new ModelBatch();light=new Environment();
        light.set(new ColorAttribute(ColorAttribute.AmbientLight,.82f,.82f,.82f,1));
        light.add(new com.badlogic.gdx.graphics.g3d.environment.DirectionalLight().set(.35f,.35f,.35f,-.3f,-1f,-.4f));
    }
    void draw(Camera camera){
        init();
        float length=HandRig.sleeveLength(Vr.current==null?5:WristHotbar.count(Vr.current.player));
        if(Math.abs(length-builtSleeveLength)>.0001f){
            if(sleeve!=null)sleeve.dispose();
            sleeve=PixelBox.create(.095f,.085f,length,new Material(ColorAttribute.createDiffuse(Color.WHITE),TextureAttribute.createDiffuse(sleeveTexture)),32);
            for(int i=0;i<2;i++)arms[i]=new ModelInstance(sleeve);
            builtSleeveLength=length;
        }
        batch.begin(camera);
        for(int i=0;i<2;i++){
            Vr.Hand h=Vr.hands[i];if(!h.valid)continue;
            // Both meshes share the tracked grip transform. The sleeve extends
            // towards the elbow (+Z), under the left forearm hotbar.
            palms[i].transform.set(h.world).translate(0,-.012f,HandRig.PALM_LENGTH*.5f);
            arms[i].transform.set(h.world).translate(0,-.012f,HandRig.PALM_LENGTH+length*.5f);
            cuffs[i].transform.set(h.world).translate(0,-.012f,HandRig.PALM_LENGTH+.009f);
            if(!VrSettings.handsOnly){batch.render(arms[i],light);batch.render(cuffs[i],light);}batch.render(palms[i],light);
        }
        batch.end();
    }
    void dispose(){if(batch!=null)batch.dispose();if(skin!=null)skin.dispose();if(sleeve!=null)sleeve.dispose();if(cuff!=null)cuff.dispose();if(skinTexture!=null)skinTexture.dispose();if(sleeveTexture!=null)sleeveTexture.dispose();if(cuffTexture!=null)cuffTexture.dispose();}
}
