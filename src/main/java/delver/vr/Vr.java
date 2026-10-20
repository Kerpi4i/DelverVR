package delver.vr;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.decals.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.math.*;
import com.interrupt.dungeoneer.GameManager;
import com.interrupt.dungeoneer.entities.*;
import com.interrupt.dungeoneer.entities.items.*;
import com.interrupt.dungeoneer.game.*;
import com.interrupt.dungeoneer.game.Game;
import com.interrupt.dungeoneer.gfx.GlRenderer;
import org.lwjgl.openvr.*;
import org.lwjgl.system.MemoryStack;
import java.nio.*;
import java.lang.reflect.Method;
import java.util.*;
import static org.lwjgl.openvr.VR.*;
import static org.lwjgl.openvr.VRSystem.*;
import static org.lwjgl.openvr.VRCompositor.*;

public final class Vr {
    public static boolean shooting;
    public static final Vector3 shotPosition=new Vector3(),shotDirection=new Vector3();
    static final boolean SIMULATE=Boolean.getBoolean("delver.vr.simulate");
    static final float SCALE=.5f;
    static final Hand[] hands={new Hand(),new Hand()};
    static final Matrix4 head=new Matrix4(), origin=new Matrix4();
    static final Matrix4[] eyes={new Matrix4(),new Matrix4()}, projections={new Matrix4(),new Matrix4()};
    static final Vector3 zero=new Vector3();
    static final FrameBuffer[] buffers=new FrameBuffer[2];
    static TrackedDevicePose.Buffer poses;
    static org.lwjgl.openvr.Texture submit;
    static PerspectiveCamera activeCamera;
    static GlRenderer renderer;
    static Game current;
    static Method drawOriginal;
    static Method pickupItem,drawNativeUi;
    static ShapeRenderer shapes;
    static SpriteBatch sprites;
    static BitmapFont font;
    static DecalBatch decals;
    static DecalBatch wristDecals;
    static WristGroupStrategy wristStrategy;
    static CameraGroupStrategy decalStrategy;
    static final PerspectiveCamera menuCamera=new PerspectiveCamera();
    static com.badlogic.gdx.graphics.Texture white;
    static com.badlogic.gdx.utils.Array<Entity> interactionEntities=new com.badlogic.gdx.utils.Array<Entity>();
    static final WristHotbar hotbar=new WristHotbar();
    static final BlockHands blockHands=new BlockHands();
    static final WristMap wristMap=new WristMap();
    static com.badlogic.gdx.graphics.Texture menuTexture;
    static FrameBuffer nativeUiFrame;
    static boolean initialized, tracked, calibrated, scene, turnLatch, menuPressed;
    static int eye=-1, width,height;
    static float yaw, referenceHeight, cooldown;
    static long lastTime;
    static float dt;
    static Player lastPlayer;
    static final Vector3 reference=new Vector3();
    static final StepSmoothing stepSmoothing=new StepSmoothing();
    static final PostProcessing postProcessing=new PostProcessing();
    static float lastBodyZ=Float.NaN,reportedRise;

    static final class Hand {
        final Matrix4 local=new Matrix4(), world=new Matrix4();
        final Vector3 pos=new Vector3(), previous=new Vector3(), tip=new Vector3(), oldTip=new Vector3(), velocity=new Vector3();
        final Vector3 throwVelocity=new Vector3();
        final VRControllerState state=VRControllerState.calloc();
        final Set<Entity> hit=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
        boolean valid, wasValid, grip, oldGrip, trigger, oldTrigger, button, oldButton, swinging, drawingBow;
        float bowDraw;
        int device=-1; float stickX,stickY,speed;
        Item held;
    }

    public static void init() {
        try(MemoryStack s=MemoryStack.stackPush()) {
            if(!SIMULATE) {
            IntBuffer error=s.callocInt(1);
            int token=VR_InitInternal(error,EVRApplicationType_VRApplication_Scene);
            if(error.get(0)!=0) throw new IllegalStateException("SteamVR init: "+VR_GetVRInitErrorAsEnglishDescription(error.get(0)));
            OpenVR.create(token);
            initialized=true;
            VRCompositor_SetTrackingSpace(ETrackingUniverseOrigin_TrackingUniverseStanding);
            IntBuffer w=s.callocInt(1),h=s.callocInt(1);
            VRSystem_GetRecommendedRenderTargetSize(w,h);
            width=w.get(0); height=h.get(0);
            } else {width=960;height=960;initialized=true;}
            for(int i=0;i<2;i++) {
                buffers[i]=new FrameBuffer(Pixmap.Format.RGBA8888,width,height,true,true);
                if(SIMULATE){eyes[i].idt().translate(i==0?-.032f:.032f,0,0);projections[i].setToProjection(.025f,100,95,1);continue;}
                HmdMatrix34 e=HmdMatrix34.calloc(s);
                VRSystem_GetEyeToHeadTransform(i,e); eyes[i]=matrix(e);
                HmdMatrix44 p=HmdMatrix44.calloc(s);
                VRSystem_GetProjectionMatrix(i,.025f,100f,p);
                for(int r=0;r<4;r++) for(int c=0;c<4;c++) projections[i].val[c*4+r]=p.m(r*4+c);
            }
            poses=TrackedDevicePose.calloc(k_unMaxTrackedDeviceCount);
            submit=org.lwjgl.openvr.Texture.calloc().eType(ETextureType_TextureType_OpenGL).eColorSpace(EColorSpace_ColorSpace_Gamma);
            shapes=new ShapeRenderer(); sprites=new SpriteBatch(); font=new BitmapFont();
            Pixmap pixel=new Pixmap(1,1,Pixmap.Format.RGBA8888);pixel.setColor(Color.WHITE);pixel.fill();white=new com.badlogic.gdx.graphics.Texture(pixel);pixel.dispose();
            menuTexture=new com.badlogic.gdx.graphics.Texture(Gdx.graphics.getWidth(),Gdx.graphics.getHeight(),Pixmap.Format.RGBA8888);
            nativeUiFrame=new FrameBuffer(Pixmap.Format.RGBA8888,Gdx.graphics.getWidth(),Gdx.graphics.getHeight(),false);
            drawOriginal=GlRenderer.class.getMethod("vrRenderOriginal",Game.class);
            drawNativeUi=GlRenderer.class.getMethod("vrDrawNativeUi");
            pickupItem=Item.class.getDeclaredMethod("pickup",Player.class);pickupItem.setAccessible(true);
            NativeUi.install();
            Gdx.app.log("DelverVR",(SIMULATE?"SIMULATION, no headset: ":"SteamVR ready: ")+width+"x"+height+" per eye");
        } catch(Exception e) { dispose(); throw new RuntimeException("Delver VR could not start. Start SteamVR and connect Quest Link/Air Link first.",e); }
    }
    static Matrix4 matrix(HmdMatrix34 m) {
        float[] v=new float[12]; for(int i=0;i<12;i++)v[i]=m.m(i); return Spatial.fromRows(v);
    }
    public static void begin() {
        long now=System.nanoTime();dt=lastTime==0?1/90f:MathUtils.clamp((now-lastTime)*1e-9f,.001f,.05f);lastTime=now;
        scene=false;eye=-1;
        int result=SIMULATE?0:VRCompositor_WaitGetPoses(poses,null);
        tracked=SIMULATE||(result==0&&poses.get(0).bPoseIsValid()&&poses.get(0).bDeviceIsConnected());
        if(!tracked) { for(Hand h:hands){h.valid=false;h.wasValid=false;h.grip=false;h.trigger=false;} return; }
        if(SIMULATE)head.idt().translate(0,1.65f,0);else head.set(matrix(poses.get(0).mDeviceToAbsoluteTracking()));
        if(!calibrated||Gdx.input.isKeyJustPressed(Input.Keys.F2)) recenter();
        for(int i=0;i<2;i++) readHand(i);
        boolean pressed=hands[0].valid&&((hands[0].state.ulButtonPressed()&(1L<<EVRButtonId_k_EButton_ApplicationMenu))!=0||hands[0].button);
        if(pressed&&!menuPressed)togglePause();menuPressed=pressed;
        current=GameManager.getGame();
        NativeUi.update();
        com.interrupt.dungeoneer.GameApplication app=com.interrupt.dungeoneer.GameApplication.instance;
        if(app!=null&&app.getScreen()==app.mainScreen&&current!=null&&current.player!=null&&current.level!=null) {
            if(lastPlayer!=current.player){ for(Hand h:hands)h.held=null;lastPlayer=current.player;recenter(); }
            updateGame();
        }
    }
    static void togglePause(){
        com.interrupt.dungeoneer.overlays.OverlayManager overlays=com.interrupt.dungeoneer.overlays.OverlayManager.instance;
        if(overlays.current() instanceof com.interrupt.dungeoneer.overlays.PauseOverlay)overlays.pop();
        else {
            Game game=GameManager.getGame();if(game==null||game.player==null)return;
            if(game.getShowingMenu()){game.hideMenu();NativeUi.cancelClick();NativeUi.update();return;}
            overlays.push(new com.interrupt.dungeoneer.overlays.PauseOverlay());
        }
        NativeUi.update();
    }
    static void recenter() {head.getTranslation(reference);referenceHeight=reference.y;calibrated=true;stepSmoothing.reset();lastBodyZ=current!=null&&current.player!=null?current.player.z:Float.NaN;reportedRise=0;for(Hand h:hands)h.wasValid=false;}
    public static void onStep(Actor actor,float rise){
        if(current!=null&&actor==current.player&&rise>0&&rise<=Locomotion.stepHeight(current.player)+.05f){stepSmoothing.step(rise);reportedRise+=rise;}
    }
    public static void onLadder(Player p){
        if(!tracked||current==null||current.player!=p||!p.isOnLadder)return;
        Hand hand=hands[0];
        p.za=!p.isDead&&!pMenu()&&hand.valid?Locomotion.climb(hand.stickX,hand.stickY,p.isOnFloor):0;
    }
    static void finishHeight(Player p){
        float rise=p.z-lastBodyZ,limit=Locomotion.stepHeight(p)+.05f;
        // Floor clamps, small edges and moving supports can bypass Actor.stepUp.
        // Compare the net physics rise and credit only the part not already reported.
        if(!Float.isNaN(lastBodyZ)&&rise>0&&rise<=limit&&!p.floating&&!p.isOnLadder&&p.za<=.001f)
            stepSmoothing.step(Math.max(0,rise-reportedRise));
        else if(!Float.isNaN(lastBodyZ)&&Math.abs(rise)>Math.max(1,limit*2))stepSmoothing.reset();
        lastBodyZ=p.z;reportedRise=0;
        stepSmoothing.tick(dt);
    }
    static void readHand(int i) {
        Hand h=hands[i];h.oldGrip=h.grip;h.oldTrigger=h.trigger;h.oldButton=h.button;
        if(SIMULATE){h.valid=true;if(i==0)h.local.idt().translate(-.05f,1.35f,-.5f).rotate(Vector3.Z,-90).rotate(Vector3.X,90);else h.local.idt().translate(.25f,1.25f,-.4f);return;}
        h.device=VRSystem_GetTrackedDeviceIndexForControllerRole(i==0?ETrackedControllerRole_TrackedControllerRole_LeftHand:ETrackedControllerRole_TrackedControllerRole_RightHand);
        h.valid=h.device>=0&&h.device<poses.capacity()&&poses.get(h.device).bPoseIsValid()&&poses.get(h.device).bDeviceIsConnected();
        if(!h.valid||!VRSystem_GetControllerState(h.device,h.state)){h.valid=false;h.grip=false;h.trigger=false;h.button=false;h.stickX=h.stickY=0;h.wasValid=false;return;}
        h.local.set(matrix(poses.get(h.device).mDeviceToAbsoluteTracking()));
        long b=h.state.ulButtonPressed();
        h.grip=h.state.rAxis(2).x()>.55f||(b&(1L<<EVRButtonId_k_EButton_Grip))!=0;
        h.trigger=h.state.rAxis(1).x()>.65f||(b&(1L<<EVRButtonId_k_EButton_SteamVR_Trigger))!=0;
        h.button=(b&(1L<<EVRButtonId_k_EButton_A))!=0;
        h.stickX=h.state.rAxis(0).x();h.stickY=h.state.rAxis(0).y();
    }
    static float dead(float x){return Math.abs(x)<.18f?0:x;}
    static void updateGame() {
        Player p=current.player;
        if(p.isDead||current.gameOver||com.interrupt.dungeoneer.overlays.OverlayManager.instance.current()!=null)return;
        cooldown=Math.max(0,cooldown-dt);
        if(hands[1].valid&&Math.abs(hands[1].stickX)>.7f&&!turnLatch){yaw-=Math.signum(hands[1].stickX)*30;turnLatch=true;}
        if(Math.abs(hands[1].stickX)<.3f)turnLatch=false;
        if(hands[1].button&&!hands[1].oldButton)recenter();
        Vector3 hp=head.getTranslation(new Vector3());
        Vector3 room=new Vector3(hp.x-reference.x,0,hp.z-reference.z).scl(SCALE).rotate(Vector3.Y,yaw);
        if(!pMenu())move(p,room.x,room.z);
        reference.x=hp.x;reference.z=hp.z;
        Vector3 f=new Vector3(0,0,-1).rot(head);f.y=0;f.nor().rotate(Vector3.Y,yaw);
        Vector3 right=new Vector3(f).crs(Vector3.Y).nor();
        if(!pMenu()&&hands[0].valid) {
            float gain=Locomotion.stick(hands[0].stickX,hands[0].stickY);
            Vector3 delta=new Vector3(f).scl(hands[0].stickY).mulAdd(right,hands[0].stickX).scl(gain*Locomotion.speed(p)*dt);
            // While attached, forward/back drives vertical physics instead of walking
            // away from the ladder. Lateral input still lets the player dismount.
            if(p.isOnLadder)delta.set(right).scl(hands[0].stickX*gain*Locomotion.speed(p)*dt*.5f);
            move(p,delta.x,delta.z);
        }
        if(!pMenu()){p.rot=(float)Math.atan2(f.x,f.z);p.yrot=0;p.rota=0;p.rotya=0;}
        setOrigin(p);
        interactionEntities=Targets.all(current.level,renderer);
        for(int i=0;i<2;i++) {
            Hand h=hands[i];if(!h.valid)continue;
            h.previous.set(h.pos);h.oldTip.set(h.tip);
            h.world.set(origin).mul(h.local);h.world.getTranslation(h.pos);
            h.tip.set(HeldVisual.tip(h.world,h.held));
            boolean fresh=!h.wasValid;
            if(fresh){h.previous.set(h.pos);h.oldTip.set(h.tip);h.throwVelocity.setZero();h.hit.clear();h.swinging=false;}
            h.velocity.set(h.pos).sub(h.previous).scl(1/dt);
            if(!fresh){
                Vector3 measured=new Vector3(h.velocity);
                if(!SIMULATE&&h.device>=0){HmdVector3 v=poses.get(h.device).vVelocity();measured.set(v.v(0),v.v(1),v.v(2)).scl(SCALE).rotate(Vector3.Y,yaw);}
                h.throwVelocity.lerp(measured,Math.min(1,dt*35));
            }
            h.speed=h.tip.dst(h.oldTip)/dt/SCALE;h.wasValid=true;
            if(!Spatial.finite(h.pos)||h.pos.dst(new Vector3(p.x,p.z+p.eyeHeight,p.y))>1.6f){h.wasValid=false;continue;}
            if(h.held!=null&&!p.inventory.contains(h.held,true))h.held=null;
            if(pMenu()||NativeUi.waitRelease)continue;
            if(h.grip&&!h.oldGrip)grab(i);
            if(!h.grip&&h.oldGrip&&h.held!=null)release(i);
            if(h.trigger&&!h.oldTrigger)activate(i);
            if(h.drawingBow&&h.held instanceof Bow){Hand other=hands[1-i];if(other.valid)h.bowDraw=MathUtils.clamp(h.pos.dst(other.pos)/SCALE/.6f,0,1);if(!h.trigger&&h.oldTrigger){fire(h,h.bowDraw);h.drawingBow=false;}}
            melee(h);
        }
    }
    static boolean pMenu(){return current!=null&&current.getShowingMenu();}
    static void lightBomb(Item item){if(item instanceof FusedBomb&&!((FusedBomb)item).isLit)item.onChargeStart();}
    static void move(Player p,float dx,float dz) {
        Locomotion.move(current.level,p,dx,dz);
    }
    static void grab(int i) {
        Hand h=hands[i];Player p=current.player;
        if(i==1&&takeHotbar())return;
        Entity nearest=target(h,true);
        if(nearest!=null) {
            pickup(i,(Item)nearest,true);
        } else if(i==1&&p.GetHeldItem()!=null) {claim(h,p.GetHeldItem());pulse(h);}
    }
    static void pickup(int i,Item item,boolean hold){
        Hand h=hands[i];Player p=current.player;
        if(item.canPickup(p)){
                Integer oldHeld=p.heldItem,oldSelected=p.selectedBarItem;
                // Explicitly catching an item also catches one still settling on the floor.
                float vx=item.xa,vy=item.ya,vz=item.za;item.xa=item.ya=item.za=0;
                // doPickup is the automatic-pickup path: Potion/Food/Scroll
                // override it to consume immediately. Grasping uses pickup only.
                try{pickupItem.invoke(item,p);}catch(Exception e){throw new RuntimeException("Could not pick up "+item.getClass().getName(),e);}
                if(p.inventory.contains(item,true)){
                    if(hold){claim(h,item);p.ChangeHeldItem(p.inventory.indexOf(item,true),false);}
                    else{p.ChangeHeldItem(oldHeld,false);p.selectedBarItem=oldSelected;}
                    pulse(h);
                }
                else if(item.isActive){item.xa=vx;item.ya=vy;item.za=vz;}
        }
    }
    static void claim(Hand hand,Item item){for(Hand other:hands)if(other!=hand&&other.held==item)other.held=null;hand.held=item;}
    static boolean takeHotbar(){
        if(!hands[0].valid||!hands[1].valid)return false;
        Player p=current.player;int slot=WristHotbar.hover(hands[0].world,hands[1].pos,WristHotbar.count(p));
        if(WristHotbar.inventoryButton(hands[0].world,hands[1].pos,WristHotbar.count(p))){pToggleInventory();pulse(hands[1]);return true;}
        if(slot<0)return false;
        Item item=p.inventory.get(slot);
        if(item==null){hands[1].held=null;p.ChangeHeldItem(null,false);}
        else{claim(hands[1],item);p.ChangeHeldItem(slot,false);}
        pulse(hands[1]);return true;
    }
    static void pToggleInventory(){current.toggleInventory();NativeUi.update();}
    static void release(int i) {
        Hand h=hands[i];Player p=current.player;Item item=h.held;
        // Releasing at the chest/backpack stores the actual inventory item.
        Vector3 chest=new Vector3(0,-.22f,.10f).mul(new Matrix4(origin).mul(head));
        boolean atHotbar=i==1&&hands[0].valid&&WristHotbar.hover(hands[0].world,h.pos,WristHotbar.count(p))>=0;
        float speed=h.throwVelocity.len()/SCALE;boolean throwing=speed>.8f;
        if(pMenu()||atHotbar||(!throwing&&h.pos.dst(chest)<.22f)){h.held=null;pulse(h);return;}
        if(!clear(new Vector3(p.x,p.z+p.eyeHeight,p.y),h.pos)){h.held=null;return;}
        p.dropItem(item,current.level,0);
        item.x=h.pos.x;item.y=h.pos.z;item.z=h.pos.y+.5f;
        // The engine integrates velocities per 60 Hz tick.
        Vector3 v=throwing?Throwing.velocity(h.throwVelocity):new Vector3(h.throwVelocity).scl(1/60f);
        item.xa=v.x;item.ya=v.z;item.za=v.y;item.physicsSleeping=false;
        if(throwing)item.tossItem(current.level,Throwing.power(h.throwVelocity));
        h.held=null;pulse(h);
    }
    static void activate(int i) {
        Hand h=hands[i];Player p=current.player;
        if(h.held instanceof FusedBomb){lightBomb(h.held);pulse(h);return;}
        if(i==1&&takeHotbar())return;
        // A door or conversation takes priority over consuming/firing a held item.
        Entity nearest=target(h,false);
        if(nearest!=null){Vector3 dir=new Vector3(0,0,-1).rot(h.world).nor();nearest.use(p,dir.x,dir.z);pulse(h);return;}
        {Entity item=target(h,true);if(item!=null){pickup(i,(Item)item,false);return;}}
        if(h.held instanceof Bow){h.drawingBow=true;h.bowDraw=0;return;}
        if(h.held instanceof Wand||h.held instanceof Bow) {
            fire(h,1f);
            return;
        }
        if(h.held!=null&&!(h.held instanceof Weapon)){h.held.inventoryUse(p);return;}
    }
    static Entity target(Hand h,boolean items) {
        Vector3 direction=new Vector3(0,0,-1).rot(h.world).nor();
        Entity best=null;float distance=Float.MAX_VALUE;
        for(Entity e:interactionEntities){
            if(e==current.player||!e.isActive||(items?(!(e instanceof Item)||e.hidden):(e instanceof Item||!Targets.usable(e))))continue;
            Vector3 hit=e instanceof Stairs?Targets.stairContact((Stairs)e,current.player,h.pos,direction):Targets.contact(Targets.bounds(current.level,e),h.pos,direction,.16f,.85f);
            if(hit==null||!clear(h.pos,hit))continue;
            float d=hit.dst2(h.pos);if(d<distance){distance=d;best=e;}
        }
        return best;
    }
    static void fire(Hand h,float power){
        if(cooldown>0||power<.15f)return;Player p=current.player;Weapon w=(Weapon)h.held;
        shotDirection.set(0,0,-1).rot(h.world).nor();shotPosition.set(h.tip);
        if(!clear(h.pos,shotPosition))return;
        float x=w.x,y=w.y,z=w.z;
        try{shooting=true;w.x=shotPosition.x;w.y=shotPosition.z;w.z=shotPosition.y+.5f;p.playAttackAnimation(w,power);w.doAttack(p,current.level,power);cooldown=.4f;pulse(h);}
        finally{shooting=false;w.x=x;w.y=y;w.z=z;}
    }
    static boolean clear(Vector3 a,Vector3 b) {
        int steps=Math.max(1,(int)(a.dst(b)/.035f));Vector3 probe=new Vector3(.015f,.015f,.015f);
        // Rendering subtracts 0.5 from physics Z. Convert back before querying tiles.
        for(int s=1;s<steps;s++){Vector3 q=new Vector3(a).lerp(b,(float)s/steps);if(!current.level.isFree(q.x,q.z,q.y+.5f,probe,0,false,null))return false;}return true;
    }
    static void melee(Hand h) {
        if(!(h.held instanceof Weapon)||h.held instanceof Bow||h.held instanceof Wand)return;
        if(h.speed<.45f){h.swinging=false;h.hit.clear();return;}
        if(h.speed<1.2f||h.speed>14f||h.tip.dst(h.oldTip)>.3f)return;
        h.swinging=true;Weapon w=(Weapon)h.held;
        for(Entity e:current.level.entities) {
            if(e==current.player||e instanceof Item||!e.isActive||h.hit.contains(e))continue;
            com.badlogic.gdx.math.collision.BoundingBox bounds=Targets.bounds(current.level,e);
            Vector3 center=bounds.getCenter(new Vector3());
            float radius=Math.max(.08f,Math.max(e.collision.x,e.collision.y))+.035f;
            boolean contact=false;
            for(int j=0;j<=6&&!contact;j++) {
                Vector3 a=new Vector3(h.previous).lerp(h.oldTip,j/6f),b=new Vector3(h.pos).lerp(h.tip,j/6f);
                Vector3 target=new Vector3(center);target.y=MathUtils.clamp((a.y+b.y)/2,bounds.min.y,bounds.max.y);
                contact=Spatial.segmentDistance2(target,a,b)<radius*radius&&clear(h.pos,target);
            }
            if(contact){Vector3 dir=new Vector3(h.velocity).nor();e.hit(dir.x,dir.z,w.doAttackRoll(MathUtils.clamp(h.speed/3f,.25f,1f),current.player),w.knockback,w.getDamageType(),current.player);h.hit.add(e);w.wasUsed();pulse(h);}
        }
    }
    static void pulse(Hand h){if(!SIMULATE&&h.device>=0)VRSystem_TriggerHapticPulse(h.device,0,(short)900);}

    public static void renderScene(GlRenderer r,Game g) {
        if(!tracked)return;
        renderer=r;current=g;scene=true;
        // Native physics has now resolved terrain steps. Refresh the whole visual rig
        // once, before either eye, so the hands and wrist panels move with the head.
        if(g.player!=null){
            finishHeight(g.player);
            setOrigin(g.player);
            for(Hand h:hands)if(h.valid){h.world.set(origin).mul(h.local);h.world.getTranslation(h.pos);h.tip.set(HeldVisual.tip(h.world,h.held));}
        }
        if(hands[0].valid)hotbar.prepare(g.player);
        if(hands[1].valid)wristMap.prepare(r,g);
        Options.instance.headBobEnabled=false;
        float clock=GlRenderer.time;
        float[] firstLights=null,firstColors=null;
        RenderPreparation.begin();
        ShadowBatch.begin();
        try {
            for(eye=0;eye<2;eye++){
                buffers[eye].begin();drawOriginal.invoke(r,g);buffers[eye].end();
                postProcessing.apply(buffers[eye],eye);
                if(Boolean.getBoolean("delver.vr.regression")){
                    if(eye==0){firstLights=GlRenderer.lightPositions.clone();firstColors=GlRenderer.lightColors.clone();clock=GlRenderer.time;}
                    else if(clock!=GlRenderer.time||!Arrays.equals(firstLights,GlRenderer.lightPositions)||!Arrays.equals(firstColors,GlRenderer.lightColors))throw new AssertionError("Eyes received different light snapshots or animation times");
                }
            }
        } catch(Exception e){throw new RuntimeException("Stereo renderer failed",e);}
        finally {eye=-1;RenderPreparation.end();}
        if(g.getShowingMenu())try{
            if(nativeUiFrame.getWidth()!=Gdx.graphics.getWidth()||nativeUiFrame.getHeight()!=Gdx.graphics.getHeight()){nativeUiFrame.dispose();nativeUiFrame=new FrameBuffer(Pixmap.Format.RGBA8888,Gdx.graphics.getWidth(),Gdx.graphics.getHeight(),false);}
            nativeUiFrame.begin();Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);Gdx.gl.glClearColor(.01f,.014f,.02f,.58f);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            try{drawNativeUi.invoke(r);}finally{nativeUiFrame.end();}
        }catch(Exception e){throw new RuntimeException("Native inventory rendering failed",e);}
    }
    public static boolean secondEye(){return eye==1;}
    static void setOrigin(Player p){origin.idt().translate(p.x,p.z+stepSmoothing.offset+p.eyeHeight-referenceHeight*SCALE,p.y).rotate(Vector3.Y,yaw).scale(SCALE,SCALE,SCALE).translate(-reference.x,0,-reference.z);}
    public static void lightSortPosition(Vector3 position){if(eye>=0)new Matrix4(origin).mul(head).getTranslation(position);}
    public static void bindEye(){if(eye>=0){buffers[eye].begin();}}
    public static void camera(PerspectiveCamera c) {
        if(eye<0||!tracked)return;
        Matrix4 world=new Matrix4(origin).mul(head).mul(eyes[eye]);
        world.getTranslation(c.position);c.direction.set(0,0,-1).rot(world).nor();c.up.set(0,1,0).rot(world).nor();
        c.near=.025f;c.projection.set(projections[eye]);
        c.view.setToLookAt(c.position,new Vector3(c.position).add(c.direction),c.up);
        c.combined.set(c.projection).mul(c.view);c.invProjectionView.set(c.combined).inv();c.frustum.update(c.invProjectionView);
        activeCamera=c;
    }
    public static void drawHands() {
        if(eye<0||activeCamera==null)return;
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);Gdx.gl.glDepthMask(true);Gdx.gl.glDisable(GL20.GL_POLYGON_OFFSET_FILL);
        blockHands.draw(activeCamera);
        useDecals(activeCamera);
        for(Hand h:hands)if(h.valid&&h.held!=null){
            TextureRegion icon=h.held.getHeldInventoryTextureRegion(0);
            if(icon!=null){Vector2 size=HeldVisual.dimensions(h.held);Decal d=Decal.newDecal(size.x,size.y,icon,true);d.setPosition(HeldVisual.center(h.world,h.held));d.setRotation(HeldVisual.orientation(h.world,h.held).getRotation(new Quaternion(),true));decals.add(d);}
        }
        // Alpha-zero sprite pixels must not write invisible occluders into depth.
        Gdx.gl.glDepthMask(false);decals.flush();Gdx.gl.glDepthMask(true);
        if(wristDecals==null){wristStrategy=new WristGroupStrategy(activeCamera);wristDecals=new DecalBatch(wristStrategy);}else wristStrategy.setCamera(activeCamera);
        hotbar.draw(current.player);wristMap.draw();wristDecals.flush();
    }
    static void useDecals(com.badlogic.gdx.graphics.Camera camera){if(decals==null){decalStrategy=new CameraGroupStrategy(camera);decals=new DecalBatch(decalStrategy);}else decalStrategy.setCamera(camera);}
    public static void finish() {
        if(!initialized)return;
        if(!scene||pMenu()||com.interrupt.dungeoneer.overlays.OverlayManager.instance.current()!=null){renderMenu();}
        if(!SIMULATE)for(int i=0;i<2;i++) {
            submit.handle(buffers[i].getColorBufferTexture().getTextureObjectHandle());
            int result=VRCompositor_Submit(i,submit,null,EVRSubmitFlags_Submit_Default);
            if(result!=0&&result!=EVRCompositorError_VRCompositorError_DoNotHaveFocus)Gdx.app.log("DelverVR","Compositor submit error "+result);
        }
        if(!SIMULATE)VRCompositor_PostPresentHandoff();
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);sprites.setProjectionMatrix(new Matrix4().setToOrtho2D(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight()));
        // Framebuffer textures have their bottom row at v=0; SpriteBatch's
        // ordinary Texture overload assumes an image with its top row at v=0.
        sprites.begin();sprites.draw(framebufferRegion(buffers[0].getColorBufferTexture()),0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());sprites.end();
    }
    static TextureRegion framebufferRegion(com.badlogic.gdx.graphics.Texture texture) {
        TextureRegion region=new TextureRegion(texture);
        region.flip(false,true);
        return region;
    }
    static void renderMenu() {
        // Capture the game's original menus and show them as a tracked, stereo plane.
        boolean transparent=scene&&pMenu();
        com.badlogic.gdx.graphics.Texture uiTexture=transparent?nativeUiFrame.getColorBufferTexture():menuTexture;
        if(!transparent){menuTexture.bind();Gdx.gl.glCopyTexImage2D(GL20.GL_TEXTURE_2D,0,GL20.GL_RGBA,0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight(),0);}
        PerspectiveCamera cam=menuCamera;
        Vector3 center=NativeUi.plane.getTranslation(new Vector3());
        useDecals(cam);
        Hand h=hands[1];
        for(eye=0;eye<2;eye++){
            buffers[eye].begin();Gdx.gl.glClearColor(.015f,.015f,.025f,1);Gdx.gl.glClear(transparent?GL20.GL_DEPTH_BUFFER_BIT:GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
            Matrix4 transform=new Matrix4(head).mul(eyes[eye]);transform.getTranslation(cam.position);cam.direction.set(0,0,-1).rot(transform).nor();cam.up.set(0,1,0).rot(transform).nor();
            cam.projection.set(projections[eye]);cam.view.setToLookAt(cam.position,new Vector3(cam.position).add(cam.direction),cam.up);cam.combined.set(cam.projection).mul(cam.view);
            Decal d=Decal.newDecal(2.4f,1.35f,framebufferRegion(uiTexture),true);d.setPosition(center);d.setRotation(NativeUi.plane.getRotation(new Quaternion(),true));decals.add(d);decals.flush();
            if(h.valid&&NativeUi.inside){Vector3 start=h.local.getTranslation(new Vector3());shapes.setProjectionMatrix(cam.combined);shapes.begin(ShapeRenderer.ShapeType.Line);shapes.setColor(Color.CYAN);shapes.line(start,NativeUi.pointerPoint);shapes.end();}
            buffers[eye].end();
        }eye=-1;
    }
    public static void dispose() {
        ShadowBatch.dispose();
        postProcessing.dispose();
        hotbar.dispose();
        wristMap.dispose();
        if(nativeUiFrame!=null)nativeUiFrame.dispose();
        blockHands.dispose();
        if(wristDecals!=null)wristDecals.dispose();
        for(FrameBuffer b:buffers)if(b!=null)b.dispose();
        if(shapes!=null)shapes.dispose();if(sprites!=null)sprites.dispose();if(font!=null)font.dispose();if(menuTexture!=null)menuTexture.dispose();if(decals!=null)decals.dispose();if(white!=null)white.dispose();
        if(poses!=null)poses.free();if(submit!=null)submit.free();for(Hand h:hands)h.state.free();
        if(initialized){if(!SIMULATE)VR_ShutdownInternal();initialized=false;}
    }
}



