package delver.vr;
import com.badlogic.gdx.math.*;
/** Match Delver's full-charge 0.3/tick throw, rather than treating its gravity as SI. */
public final class Throwing {
    public static float power(Vector3 velocity){
        float speed=velocity.len()/Vr.SCALE;
        float effort=MathUtils.clamp((speed-.65f)/(4.5f-.65f),0,1);
        return (float)Math.pow(effort,1.6);
    }
    public static Vector3 velocity(Vector3 controller){
        float power=power(controller);
        if(controller.len2()<.00001f)return new Vector3();
        return new Vector3(controller).nor().scl(.3f*power).add(0,.05f*power,0);
    }
}
