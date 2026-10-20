package delver.vr;
/** Physical dimensions in tracking metres; the grip collider is the front of the palm. */
public final class HandRig {
    static final float PALM_LENGTH=.15f, TOP=.031f;
    static float sleeveLength(int slots){return Math.max(.22f,(slots+1)*WristHotbar.CELL+.024f);}
    static float hotbarCenter(int slots){return PALM_LENGTH+(slots*.5f+1)*WristHotbar.CELL+.012f;}
}
