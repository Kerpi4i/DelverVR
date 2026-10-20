package delver.vr;
import com.interrupt.dungeoneer.entities.Player;
public final class LadderRegression {
    public static void run(){
        Player previous=Vr.current.player;
        Vr.Hand hand=Vr.hands[0];boolean valid=hand.valid;
        float x=hand.stickX,y=hand.stickY;
        try{
            Player probe=new Player();probe.x=previous.x;probe.y=previous.y;
            probe.inventory.addAll(previous.inventory);probe.inventorySize=previous.inventorySize;probe.inEditor=true;
            Vr.current.player=probe;hand.valid=true;hand.stickX=0;
            for(float input:new float[]{.95f,-.95f,0}){
                probe.z=previous.z+2;probe.za=0;probe.yrot=0;probe.isOnLadder=true;probe.isOnFloor=false;
                hand.stickY=input;
                // Exercise the patched native ladder block, not just the helper.
                probe.tick(Vr.current.level,0f);
                float expected=input>0?.02f:input<0?-.02f:0;
                if(Math.abs(probe.za-expected)>.00001f||probe.isOnLadder)
                    throw new AssertionError("Native ladder physics did not consume VR input: "+input+" -> "+probe.za);
            }
            hand.valid=false;probe.z=previous.z+2;probe.isOnLadder=true;probe.za=.02f;
            probe.tick(Vr.current.level,0f);
            if(probe.za!=0)throw new AssertionError("Climbing continued after controller tracking loss");
            com.badlogic.gdx.Gdx.app.log("DelverVR","LADDER PASS: native tick up/down/idle/tracking loss");
        }finally{Vr.current.player=previous;hand.valid=valid;hand.stickX=x;hand.stickY=y;}
    }
}
