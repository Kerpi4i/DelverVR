package delver.vr;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.interrupt.dungeoneer.gfx.drawables.DrawableMesh;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

/** CPU-only workers consume copied transforms and immutable vertex snapshots.
 * GPU uploads, resource ownership and the original stencil passes stay on the GL thread.
 */
public final class ShadowBatch {
    private static final int LIMIT=60000, MAX_VERTICES=1500000;
    private static final Field MODEL;
    static {
        try { MODEL=DrawableMesh.class.getDeclaredField("modelView"); MODEL.setAccessible(true); }
        catch(Exception e){throw new ExceptionInInitializerError(e);}
    }
    private static ExecutorService pool;
    private static final IdentityHashMap<Mesh,Source> sources=new IdentityHashMap<Mesh,Source>();
    private static final ArrayList<Slot> slots=new ArrayList<Slot>();
    private static final ArrayList<Work> workPool=new ArrayList<Work>();
    private static int used;
    private static boolean cacheReady;
    private static int lastWorkCount;
    private static String lastMode;
    private static String lastTexture;
    public static int inputMeshes,outputMeshes,workerJobs,vertices,cachedGroups;
    public static long prepareNanos,waitNanos;
    public static String mode(){return System.getProperty("delver.vr.shadowMode","original");}
    public static void begin(){used=0;inputMeshes=outputMeshes=workerJobs=vertices=cachedGroups=0;prepareNanos=waitNanos=0;}

    private static final class Source {
        final float[] data;
        final short[] indices;
        final VertexAttributes attributes;
        final int stride,position,count;
        Source(Mesh mesh){
            VertexAttributes original=mesh.getVertexAttributes();int originalStride=mesh.getVertexSize()/4;
            VertexAttribute p=original.findByUsage(VertexAttributes.Usage.Position);
            if(p==null||p.numComponents!=3)throw new IllegalArgumentException("Unsupported shadow positions");
            // These two shipped shadow OBJ files have no UVs. The stock main-dynamic
            // shader does not read normals. Preserve topology, deduplicate positions.
            for(VertexAttribute a:original)if(a.usage!=VertexAttributes.Usage.Position&&a.usage!=VertexAttributes.Usage.Normal)
                throw new IllegalArgumentException("Shadow has additional vertex attributes");
            float[] raw=new float[mesh.getNumVertices()*originalStride];mesh.getVertices(raw);
            HashMap<Position,Integer> unique=new HashMap<Position,Integer>();
            float[] compact=new float[mesh.getNumVertices()*3];int[] remap=new int[mesh.getNumVertices()];int n=0;
            for(int i=0;i<remap.length;i++){
                int o=i*originalStride+p.offset/4;Position key=new Position(raw[o],raw[o+1],raw[o+2]);Integer id=unique.get(key);
                if(id==null){id=n++;unique.put(key,id);compact[id*3]=raw[o];compact[id*3+1]=raw[o+1];compact[id*3+2]=raw[o+2];}
                remap[i]=id;
            }
            count=n;stride=3;position=0;data=Arrays.copyOf(compact,n*3);
            attributes=new VertexAttributes(new VertexAttribute(VertexAttributes.Usage.Position,3,"a_position"));
            if(mesh.getNumIndices()>0){indices=new short[mesh.getNumIndices()];mesh.getIndices(indices);for(int i=0;i<indices.length;i++)indices[i]=(short)remap[indices[i]&65535];}
            else {indices=new short[remap.length];for(int i=0;i<indices.length;i++)indices[i]=(short)remap[i];}
        }
    }
    private static final class Position {
        final int x,y,z;
        Position(float x,float y,float z){this.x=Float.floatToIntBits(x);this.y=Float.floatToIntBits(y);this.z=Float.floatToIntBits(z);}
        public int hashCode(){return (x*31+y)*31+z;}
        public boolean equals(Object other){if(!(other instanceof Position))return false;Position p=(Position)other;return x==p.x&&y==p.y&&z==p.z;}
    }
    private static final class Part {
        Source source;final float[] matrix=new float[16];int offset;
        void set(Source s,Matrix4 m,int offset){source=s;System.arraycopy(m.val,0,matrix,0,16);this.offset=offset;}
    }
    private static final class Work {
        final ArrayList<Part> parts=new ArrayList<Part>();
        Source format;
        int count,indexCount,partCount,previousPartCount;boolean changed;
        float[] data=new float[0];short[] indices=new short[0];
        void reset(Source s){format=s;previousPartCount=partCount;count=indexCount=partCount=0;changed=false;}
        void add(Source s,Matrix4 m){
            if(partCount==parts.size())parts.add(new Part());Part p=parts.get(partCount++);
            if(p.source!=s||p.offset!=count||!Arrays.equals(p.matrix,m.val))changed=true;
            p.set(s,m,count);count+=s.count;indexCount+=s.indices.length==0?s.count:s.indices.length;
        }
        void allocate(){if(data.length<count*format.stride)data=new float[count*format.stride];if(indices.length<indexCount)indices=new short[indexCount];}
        void transform(){
            int at=0;
            for(int part=0;part<partCount;part++){
                Part p=parts.get(part);
                Source s=p.source;transformVertices(s.data,p.matrix,s.stride,s.position,data,p.offset*s.stride);
                if(s.indices.length==0){for(int i=0;i<s.count;i++)indices[at++]=(short)(p.offset+i);}
                else {for(short index:s.indices)indices[at++]=(short)(p.offset+(index&65535));}
            }
        }
    }
    private static final class Slot {
        Mesh mesh;final DrawableMesh drawable=new DrawableMesh();
        int maxVertices,maxIndices;VertexAttributes attributes;
        void upload(Work w,String texture)throws Exception {
            if(mesh==null||!w.format.attributes.equals(attributes)||maxVertices<w.count||maxIndices<w.indexCount){
                if(mesh!=null)mesh.dispose();
                maxVertices=Math.max(w.count,maxVertices);maxIndices=Math.max(w.indexCount,maxIndices);attributes=w.format.attributes;
                mesh=new Mesh(false,maxVertices,maxIndices,attributes);
            }
            mesh.setVertices(w.data,0,w.count*w.format.stride);mesh.setIndices(w.indices,0,w.indexCount);
            drawable.loadedMesh=mesh;drawable.textureFile=texture;drawable.fullbrite=true;
            drawable.shader=null;drawable.bakeLighting=false;drawable.color=com.badlogic.gdx.graphics.Color.WHITE;
            if(MODEL.get(drawable)==null)MODEL.set(drawable,new Matrix4());
        }
    }

    /** Pure affine math: no entities, pools, libGDX native temporaries or GL calls. */
    public static void transformVertices(float[] source,float[] m,int stride,int position,float[] target,int offset){
        System.arraycopy(source,0,target,offset,source.length);
        for(int i=position;i<source.length;i+=stride){
            float x=source[i],y=source[i+1],z=source[i+2];int o=offset+i;
            target[o]=m[0]*x+m[4]*y+m[8]*z+m[12];
            target[o+1]=m[1]*x+m[5]*y+m[9]*z+m[13];
            target[o+2]=m[2]*x+m[6]*y+m[10]*z+m[14];
        }
    }
    private static boolean eligible(DrawableMesh m){
        return m.loadedMesh!=null&&!m.bakeLighting&&(m.shader==null||m.shader.isEmpty())&&
            ("meshes/shadows/blob.obj".equals(m.meshFile)||"meshes/shadows/rectangle.obj".equals(m.meshFile));
    }
    public static Array<DrawableMesh> prepare(Array<DrawableMesh> original){
        String mode=mode();
        if("original".equals(mode)||original==null||original.size<2)return original;
        long start=System.nanoTime();
        boolean canReuse=cacheReady&&mode.equals(lastMode);cacheReady=false;
        try {
            // Only batch an entire compatible group. Halos retain their original path.
            ArrayList<Work> work=new ArrayList<Work>();Work current=null;
            String texture=original.first().textureFile;int total=0;
            canReuse=canReuse&&Objects.equals(texture,lastTexture);
            for(DrawableMesh m:original){
                if(!eligible(m)||!Objects.equals(texture,m.textureFile))return original;
                Source s=sources.get(m.loadedMesh);
                if(s==null){s=new Source(m.loadedMesh);if(sources.size()>16)sources.clear();sources.put(m.loadedMesh,s);}
                if(s.count>LIMIT||(total+=s.count)>MAX_VERTICES)return original;
                if(current==null||current.count+s.count>LIMIT||!current.format.attributes.equals(s.attributes)){
                    if(work.size()==workPool.size())workPool.add(new Work());
                    current=workPool.get(work.size());current.reset(s);work.add(current);
                }
                Matrix4 matrix=(Matrix4)MODEL.get(m);if(matrix==null)return original;
                current.add(s,matrix);
            }
            if(work.size()>=original.size)return original;
            canReuse=canReuse&&work.size()==lastWorkCount;
            for(Work w:work)if(w.changed||w.partCount!=w.previousPartCount)canReuse=false;
            if(canReuse){
                Array<DrawableMesh> result=new Array<DrawableMesh>(work.size());
                for(int i=0;i<work.size();i++)result.add(slots.get(i).drawable);
                inputMeshes+=original.size;outputMeshes+=result.size;vertices+=total;cachedGroups++;cacheReady=true;
                return result;
            }
            for(Work w:work)w.allocate();
            if("parallel".equals(mode)&&work.size()>1){
                if(pool==null)pool=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"DelverShadowWorker");t.setDaemon(true);return t;});
                ArrayList<Future<?>> pending=new ArrayList<Future<?>>();
                // Two coarse jobs; the GL thread also computes one third of the batches.
                for(int lane=0;lane<2;lane++){
                    final int from=lane;pending.add(pool.submit(()->{for(int i=from;i<work.size();i+=3)work.get(i).transform();}));workerJobs++;
                }
                for(int i=2;i<work.size();i+=3)work.get(i).transform();
                long wait=System.nanoTime();
                for(Future<?> job:pending)job.get();
                waitNanos+=System.nanoTime()-wait;
            } else for(Work w:work)w.transform();
            Array<DrawableMesh> result=new Array<DrawableMesh>(work.size());
            used=0; // Previous stencil group has already been drawn; reuse its GPU buffers.
            for(Work w:work){
                if(used==slots.size())slots.add(new Slot());Slot slot=slots.get(used++);
                slot.upload(w,texture);result.add(slot.drawable);
            }
            inputMeshes+=original.size;outputMeshes+=result.size;vertices+=total;
            cacheReady=true;lastWorkCount=work.size();lastMode=mode;lastTexture=texture;
            return result;
        }catch(Exception e){
            if(e instanceof InterruptedException)Thread.currentThread().interrupt();
            Gdx.app.error("DelverShadowBatch","Falling back to original stencil geometry",e);return original;
        }finally{prepareNanos+=System.nanoTime()-start;}
    }
    public static String statistics(){return "shadowMode="+mode()+", shadowMeshes="+inputMeshes+"->"+outputMeshes+", shadowVertices="+vertices+", workerJobs="+workerJobs+", shadowCachedGroups="+cachedGroups+", shadowPrepareMs="+(prepareNanos/1e6)+", workerWaitMs="+(waitNanos/1e6);}
    public static void dispose(){if(pool!=null){pool.shutdownNow();pool=null;}for(Slot s:slots)if(s.mesh!=null)s.mesh.dispose();slots.clear();sources.clear();workPool.clear();cacheReady=false;}
}
