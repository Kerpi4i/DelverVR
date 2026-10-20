package delver.vr;

/** Visual offset only: physics and physical head tracking remain immediate. */
final class StepSmoothing {
    float offset,velocity;
    void reset(){offset=velocity=0;}
    void step(float rise){
        if(rise<=0)return;
        offset-=rise;
    }
    void tick(float dt){
        // Exact critically damped spring: no overshoot, independent of refresh rate.
        float omega=12f,c=velocity+omega*offset,e=(float)Math.exp(-omega*dt);
        velocity=(velocity-omega*c*dt)*e;
        offset=(offset+c*dt)*e;
        if(Math.abs(offset)<.00001f&&Math.abs(velocity)<.0001f)reset();
    }
}
