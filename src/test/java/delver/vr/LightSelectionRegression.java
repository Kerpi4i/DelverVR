package delver.vr;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.interrupt.dungeoneer.gfx.DynamicLight;
public final class LightSelectionRegression {
    public static void run(){
        Vector3 center=new Vector3(0,0,0);Array<DynamicLight> lights=new Array<DynamicLight>();
        for(int i=0;i<5;i++){DynamicLight tiny=new DynamicLight();tiny.position.set(.3f+i*.01f,0,0);tiny.color.set(1,1,1);tiny.range=.2f;lights.add(tiny);}
        DynamicLight room=new DynamicLight();room.position.set(1.5f,0,0);room.color.set(1,1,1);room.range=3;lights.add(room);
        DynamicLight off=new DynamicLight();off.position.set(0,0,0);off.color.setZero();off.range=3;lights.add(off);
        DynamicLight dark=new DynamicLight();dark.color.set(-1,0,0);dark.range=3;dark.position.set(.2f,0,0);InteractionRegression.check(LightSelection.score(dark,center)>0,"Signed mod light lost its influence");
        int eye=Vr.eye;try{Vr.eye=-1;LightSelection.sort(lights,center);}finally{Vr.eye=eye;}
        InteractionRegression.check(lights.first()==room,"Decorative short-range lights displaced the room light");
        InteractionRegression.check(lights.peek()==off,"Disabled light consumed priority");
        InteractionRegression.check(center.isZero(),"Sorting changed the native camera position");
        InteractionRegression.check(LightSelection.score(room,center)>LightSelection.score(lights.get(1),center),"Light ranking does not match shader attenuation");
        com.badlogic.gdx.Gdx.app.log("DelverVR","LIGHT SELECTION PASS: room light versus tiny decorative sources, disabled lights, unchanged camera, native attenuation");
    }
}
