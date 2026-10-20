package delver.vr;
import com.interrupt.dungeoneer.entities.Entity;
import com.interrupt.dungeoneer.gfx.drawables.DrawableMesh;
public final class RenderPreparationTest {
    static final class Probe extends Entity {
        int calls;
        public void updateDrawable(){ calls++; }
    }
    static void check(boolean ok){if(!ok)throw new AssertionError("Stereo mesh cache regression");}
    public static void main(String[] args){
        Probe a=new Probe(),b=new Probe(); a.drawable=new DrawableMesh();
        RenderPreparation.begin(); RenderPreparation.update(a); RenderPreparation.update(a);
        check(a.calls==1 && RenderPreparation.reused==1);
        b.drawable=a.drawable; RenderPreparation.update(b); RenderPreparation.update(a);
        check(a.calls==2 && b.calls==1); // Shared meshes must restore their owner transform.
        a.drawable=new DrawableMesh(); RenderPreparation.update(a); check(a.calls==3);
        RenderPreparation.end(); RenderPreparation.update(a); check(a.calls==4);
        RenderPreparation.begin(); RenderPreparation.update(a); check(a.calls==5);
        ((DrawableMesh)a.drawable).bakeLighting=true;
        RenderPreparation.update(a); RenderPreparation.update(a); check(a.calls==7);
        a.drawable=null; RenderPreparation.update(a); RenderPreparation.update(a); check(a.calls==9);
        RenderPreparation.end(); System.out.println("Stereo preparation PASS");
    }
}
