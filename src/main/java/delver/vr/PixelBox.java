package delver.vr;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.math.Vector3;

/** Repeat square texels in physical metres on each face, instead of stretching a tile. */
public final class PixelBox {
    static final float PIXELS_PER_METRE=160;
    static float span(float metres,int texturePixels){return metres*PIXELS_PER_METRE/texturePixels;}
    static Model create(float width,float height,float depth,Material material,int texturePixels){
        ModelBuilder builder=new ModelBuilder();builder.begin();
        MeshPartBuilder part=builder.part("pixel-box",GL20.GL_TRIANGLES,VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal|VertexAttributes.Usage.TextureCoordinates,material);
        float x=width*.5f,y=height*.5f,z=depth*.5f;
        face(part,width,height,texturePixels,new Vector3(-x,-y,z),new Vector3(x,-y,z),new Vector3(x,y,z),new Vector3(-x,y,z),new Vector3(0,0,1));
        face(part,width,height,texturePixels,new Vector3(x,-y,-z),new Vector3(-x,-y,-z),new Vector3(-x,y,-z),new Vector3(x,y,-z),new Vector3(0,0,-1));
        face(part,width,depth,texturePixels,new Vector3(-x,y,z),new Vector3(x,y,z),new Vector3(x,y,-z),new Vector3(-x,y,-z),new Vector3(0,1,0));
        face(part,width,depth,texturePixels,new Vector3(-x,-y,-z),new Vector3(x,-y,-z),new Vector3(x,-y,z),new Vector3(-x,-y,z),new Vector3(0,-1,0));
        face(part,depth,height,texturePixels,new Vector3(x,-y,z),new Vector3(x,-y,-z),new Vector3(x,y,-z),new Vector3(x,y,z),new Vector3(1,0,0));
        face(part,depth,height,texturePixels,new Vector3(-x,-y,-z),new Vector3(-x,-y,z),new Vector3(-x,y,z),new Vector3(-x,y,-z),new Vector3(-1,0,0));
        return builder.end();
    }
    private static void face(MeshPartBuilder p,float u,float v,int pixels,Vector3 a,Vector3 b,Vector3 c,Vector3 d,Vector3 normal){p.setUVRange(0,0,span(u,pixels),span(v,pixels));p.rect(a,b,c,d,normal);}
}
