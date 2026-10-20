package delver.vr;
import com.badlogic.gdx.math.*;
public final class SpatialTest {
    static void near(float a,float b){if(Math.abs(a-b)>1e-5)throw new AssertionError(a+" != "+b);}
    public static void main(String[] args){
        Matrix4 m=Spatial.fromRows(new float[]{0,0,1,2, 0,1,0,3, -1,0,0,4});
        Vector3 p=new Vector3(0,0,-1).mul(m);near(p.x,1);near(p.y,3);near(p.z,4);
        Vector3 a=new Vector3(-1,0,0),b=new Vector3(1,0,0);
        near(Spatial.segmentDistance2(new Vector3(0,.2f,0),a,b),.04f);
        near(Spatial.segmentDistance2(new Vector3(2,0,0),a,b),1);
        near(Spatial.segmentDistance2(new Vector3(2,0,0),a,a),9);
        if(Spatial.finite(new Vector3(Float.NaN,0,0)))throw new AssertionError("Invalid tracking pose accepted");
        if(!Spatial.finite(new Vector3(0,1,0)))throw new AssertionError("Valid pose rejected");
        float soft=Throwing.power(new Vector3(.5f,0,0)),medium=Throwing.power(new Vector3(1,0,0)),hard=Throwing.power(new Vector3(1.5f,0,0));
        if(!(soft<.1f&&medium>soft*3&&hard>medium*2&&hard<.8f))throw new AssertionError("Throw response saturates early or lacks dynamic range");
        Vector3 launch=Throwing.velocity(new Vector3(2.25f,0,0));near(launch.x,.3f);near(launch.y,.05f);
        float distance=0,height=.6f,vx=launch.x,vy=launch.y;
        for(int tick=0;tick<240&&height>=0;tick++){distance+=vx;height+=vy;vx*=.98f;vy-=.0035f;}
        if(distance<3.5f)throw new AssertionError("Full-strength throw still has a two-metre range cap");
        for(int slots:new int[]{4,5,8,10}){
            float near=HandRig.hotbarCenter(slots)-(slots*.5f+1)*WristHotbar.CELL;
            float far=HandRig.hotbarCenter(slots)+slots*.5f*WristHotbar.CELL;
            if(near<HandRig.PALM_LENGTH||far>HandRig.PALM_LENGTH+HandRig.sleeveLength(slots))throw new AssertionError("Hotbar overhangs forearm for "+slots+" slots");
        }
        near(.085f/(PixelBox.span(.085f,16)*16),.15f/(PixelBox.span(.15f,16)*16));
        near(.095f/(PixelBox.span(.095f,32)*32),.354f/(PixelBox.span(.354f,32)*32));
        near(Locomotion.stick(.1f,.1f),0);
        near(Locomotion.climb(0,.95f,false),.02f);
        near(Locomotion.climb(0,-.95f,false),-.02f);
        near(Locomotion.climb(0,-.95f,true),0);
        near(Locomotion.climb(0,.1f,false),0);
        near(Locomotion.stick(0,.95f)*.95f,1);
        near(Locomotion.stick(1,1)*(float)Math.sqrt(2),1);
        if(Locomotion.stick(0,.5f)*.5f>=1)throw new AssertionError("Stick reaches full speed too early");
        StepSmoothing smooth=new StepSmoothing();smooth.step(.3f);near(smooth.offset,-.3f);
        float last=smooth.offset;
        for(int i=0;i<90;i++){
            smooth.tick(1f/90);
            if(smooth.offset<last||smooth.offset>0||smooth.offset-last>.016f)throw new AssertionError("Step camera jumps or overshoots");
            last=smooth.offset;
        }
        if(Math.abs(smooth.offset)>.0001f)throw new AssertionError("Step smoothing does not settle");
        StepSmoothing a60=new StepSmoothing(),a120=new StepSmoothing();a60.step(.3f);a120.step(.3f);
        for(int i=0;i<30;i++)a60.tick(1f/60);
        for(int i=0;i<60;i++)a120.tick(1f/120);
        near(a60.offset,a120.offset);
        float before=a60.offset;a60.step(.2f);near(a60.offset,before-.2f);
        a60.reset();near(a60.offset,0);near(a60.velocity,0);
        System.out.println("PASS: OpenVR matrix layout, pose translation/rotation, swept contact, segment endpoints, zero-length sweep, invalid tracking.");
    }
}
