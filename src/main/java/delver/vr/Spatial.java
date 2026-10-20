package delver.vr;

import com.badlogic.gdx.math.*;

/** All VR space is metres, Y up. Delver entity space is X,Y horizontal and Z up. */
public final class Spatial {
    public static Matrix4 fromRows(float[] m) {
        Matrix4 out=new Matrix4();
        for(int r=0;r<3;r++) for(int c=0;c<4;c++) out.val[c*4+r]=m[r*4+c];
        return out;
    }
    public static float segmentDistance2(Vector3 p, Vector3 a, Vector3 b) {
        Vector3 d=new Vector3(b).sub(a);
        float t=d.len2()<1e-8f?0:MathUtils.clamp(new Vector3(p).sub(a).dot(d)/d.len2(),0,1);
        return p.dst2(new Vector3(a).mulAdd(d,t));
    }
    public static boolean finite(Vector3 p) {
        return Float.isFinite(p.x)&&Float.isFinite(p.y)&&Float.isFinite(p.z);
    }
}
