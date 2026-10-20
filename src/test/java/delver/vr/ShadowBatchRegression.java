package delver.vr;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.interrupt.dungeoneer.gfx.drawables.DrawableMesh;
import java.lang.reflect.Field;

/** Runs with the game's actual OBJ loader, native math and live GL context. */
public final class ShadowBatchRegression {
    public static void run() throws Exception {
        Field model=DrawableMesh.class.getDeclaredField("modelView");model.setAccessible(true);
        Array<DrawableMesh> originals=new Array<DrawableMesh>();
        for(int i=0;i<100;i++){
            DrawableMesh m=new DrawableMesh(i%7==0?"meshes/shadows/rectangle.obj":"meshes/shadows/blob.obj","meshes.png");
            m.x=i*.07f;m.y=(i%9)*.12f;m.z=(i%3)*.1f;
            m.rotZ=i*13;m.setScaleVector(new Vector3(.2f+(i%4)*.1f,.8f,.3f));m.update();originals.add(m);
        }
        String previous=ShadowBatch.mode();
        try {
            for(String mode:new String[]{"batch","parallel"}){
                System.setProperty("delver.vr.shadowMode",mode);ShadowBatch.begin();
                Array<DrawableMesh> combined=ShadowBatch.prepare(originals);
                if(combined==originals||combined.size>=originals.size)throw new AssertionError("Batch did not combine shipped shadow meshes");
                if(mode.equals("parallel")&&ShadowBatch.workerJobs==0)throw new AssertionError("Worker path not exercised");
                int output=0,triangle=0;
                float[] target=positions(combined.get(output).loadedMesh);short[] targetIndices=indices(combined.get(output).loadedMesh);
                for(DrawableMesh m:originals){
                    float[] source=positions(m.loadedMesh);short[] sourceIndices=indices(m.loadedMesh);Matrix4 transform=(Matrix4)model.get(m);
                    int count=sourceIndices.length==0?source.length/3:sourceIndices.length;
                    for(int i=0;i<count;i++){
                        if(triangle==targetIndices.length){output++;triangle=0;target=positions(combined.get(output).loadedMesh);targetIndices=indices(combined.get(output).loadedMesh);}
                        int s=(sourceIndices.length==0?i:sourceIndices[i]&65535)*3,t=(targetIndices[triangle++]&65535)*3;
                        Vector3 expected=new Vector3(source[s],source[s+1],source[s+2]).mul(transform);
                        if(Math.abs(expected.x-target[t])>.0001f||Math.abs(expected.y-target[t+1])>.0001f||Math.abs(expected.z-target[t+2])>.0001f)
                            throw new AssertionError("Triangle topology/transform changed: "+mode);
                    }
                }
                if(output!=combined.size-1||triangle!=targetIndices.length)throw new AssertionError("Extra batched geometry");
                int cached=ShadowBatch.cachedGroups;
                ShadowBatch.prepare(originals);
                if(ShadowBatch.cachedGroups!=cached+1)throw new AssertionError("Unchanged geometry not reused");
                originals.first().x+=.1f;originals.first().update();
                ShadowBatch.prepare(originals);
                if(ShadowBatch.cachedGroups!=cached+1)throw new AssertionError("Moving shadow reused stale geometry");
                if(Gdx.gl.glGetError()!=0)throw new AssertionError("Shadow batch GL error");
                Gdx.app.log("DelverVR","SHADOW BATCH PASS: "+ShadowBatch.statistics());
            }
        }finally{System.setProperty("delver.vr.shadowMode",previous);}
    }
    private static short[] indices(Mesh mesh){short[] r=new short[mesh.getNumIndices()];if(r.length>0)mesh.getIndices(r);return r;}
    private static float[] positions(Mesh mesh){
        int stride=mesh.getVertexSize()/4,p=mesh.getVertexAttributes().findByUsage(com.badlogic.gdx.graphics.VertexAttributes.Usage.Position).offset/4;
        float[] raw=new float[mesh.getNumVertices()*stride];mesh.getVertices(raw);float[] out=new float[mesh.getNumVertices()*3];
        for(int i=0;i<mesh.getNumVertices();i++)System.arraycopy(raw,i*stride+p,out,i*3,3);return out;
    }
}
