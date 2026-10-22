package delver.vr;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.interrupt.dungeoneer.entities.*;
import com.interrupt.dungeoneer.entities.items.Potion;
import com.interrupt.dungeoneer.entities.triggers.Trigger;
import com.interrupt.dungeoneer.game.Level;

/** Runs inside the real Steam game with its assets and physics initialized. */
public final class InteractionRegression {
    public static final class DoorProbe extends Door {
        int uses;
        public void use(Player p,float x,float y){uses++;}
    }
    public static final class TalkProbe extends Trigger {
        int uses;
        public void use(Player p,float x,float y){uses++;}
    }
    public static final class ThrowProbe extends Item {
        int tosses;float power;
        public void tossItem(Level level,float force){tosses++;power=force;}
    }
    public static final class Chunk {public Array<Entity> entities=new Array<Entity>();}
    public static final class Renderer {public Array<Chunk> chunks=new Array<Chunk>();}
    public static final class StepLevel extends Level {
        float rise,receivedStep;Entity obstacle;boolean headroom=true;
        public boolean isFree(float x,float y,float z,Vector3 c,float step,boolean floating,com.interrupt.dungeoneer.collision.Collision hit){receivedStep=step;return rise<=step;}
        public Entity getHighestEntityCollision(float x,float y,float z,Vector3 c,Entity p){return obstacle!=null&&z<obstacle.z+obstacle.collision.z-.0001f?obstacle:null;}
        public Entity checkStandingRoomWithEntities(float x,float y,float z,Vector3 c,Entity p){return null;}
        public boolean collidesWorldOrEntities(float x,float y,float z,Vector3 c,Entity p){return headroom;}
    }
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static void at(Entity e,Vector3 center){e.x=center.x;e.y=center.z;e.z=center.y+.5f-e.collision.z*.5f;e.isActive=true;}
    public static void run(){
        Level level=Vr.current.level;Player p=Vr.current.player;Vr.Hand h=Vr.hands[1];
        Matrix4 previousWorld=new Matrix4(h.world);Vector3 previousPos=new Vector3(h.pos);Item previousHeld=h.held;
        Matrix4 leftWorld=new Matrix4(Vr.hands[0].world);boolean leftValid=Vr.hands[0].valid;
        Array<Entity> previous=Vr.interactionEntities;
        Integer previousSelected=p.selectedBarItem,previousHeldSlot=p.heldItem;

        Potion item=new Potion();
        ThrowProbe throwable=new ThrowProbe();Vector3 oldThrow=new Vector3(h.throwVelocity);
        try{
            h.pos.set(p.x,p.z+p.eyeHeight,p.y);h.world.idt().translate(h.pos);h.held=null;
            Vector3 center=new Vector3(h.pos).add(0,0,-.4f);
            DoorProbe door=new DoorProbe();door.collision.set(.2f,.06f,.8f);at(door,center);
            Vr.interactionEntities=new Array<Entity>();Vr.interactionEntities.add(door);
            check(Vr.target(h,false)==door,"Aimed door was not selected by its visible bounds");
            Vr.activate(1);check(door.uses==1,"Door.use not dispatched");
            TalkProbe talk=new TalkProbe();talk.collision.set(.18f,.18f,.5f);at(talk,center);
            check(talk.hidden,"Dialogue fixture must exercise invisible USE triggers");
            Vr.interactionEntities.clear();Vr.interactionEntities.add(talk);
            Vr.activate(1);check(talk.uses==1,"Invisible dialogue USE trigger not dispatched");
            talk.triggerType=Trigger.TriggerType.PLAYER_TOUCHED;
            check(!Targets.usable(talk),"Touch-only trigger should not be activated by hand");
            talk.hidden=false;
            check(Targets.usable(talk),"Visible Workshop touch marker must accept native USE");
            Vr.activate(1);check(talk.uses==2,"Visible touch marker USE not dispatched");
            talk.hidden=true;
            Chunk chunk=new Chunk();chunk.entities.add(door);chunk.entities.add(door);
            Renderer fake=new Renderer();fake.chunks.add(chunk);
            Array<Entity> all=Targets.all(level,fake);int count=0;for(Entity e:all)if(e==door)count++;
            check(count==1,"Chunk interactions missing or duplicated");
            at(item,new Vector3(h.pos).add(0,-.08f,-.18f));item.xa=.025f;
            BoundingBox box=Targets.bounds(level,item);
            check(box.getCenter(new Vector3()).dst(new Vector3(item.x,item.z-.5f+item.collision.z*.5f,item.y))<.001f,"Item physics/render offset incorrect");
            if(!p.hasFreeInventorySpace())p.addInventorySlot();
            Vr.interactionEntities.clear();Vr.interactionEntities.add(item);
            check(Vr.target(h,true)==item,"Visible floor-item bounds not selected");
            Vr.grab(1);check(h.held==item&&p.inventory.contains(item,true)&&!item.isActive,"Explicit pickup of moving item failed");
            // A rotated/translating left controller must carry the hotbar and hit
            // regions together. Selecting a consumable must neither clone nor drink it.
            VrSettings.hotbarMode=0;Vr.hands[0].valid=true;Vr.hands[0].world.idt().translate(h.pos).rotate(Vector3.Y,37).scale(Vr.SCALE,Vr.SCALE,Vr.SCALE);
            int slots=WristHotbar.count(p);
            for(int i=0;i<slots;i++)check(WristHotbar.hover(Vr.hands[0].world,WristHotbar.slotCenter(Vr.hands[0].world,slots,i),slots)==i,"Wrist slot transform mismatch "+i);
            for(int i=0;i<slots;i++){
                Matrix4 f=WristHotbar.frame(Vr.hands[0].world);Vector3 from=WristHotbar.slotCenter(Vr.hands[0].world,slots,i).mul(new Matrix4(f).inv()).add(0,0,.25f).mul(f);
                check(WristHotbar.aim(Vr.hands[0].world,f,from,slots)==i,"Ray cannot reach wrist slot "+i);
            }
            int itemIndex=p.inventory.indexOf(item,true),slot=slots-1;
            Item displaced=p.inventory.get(slot);
            if(itemIndex!=slot){p.inventory.set(itemIndex,displaced);p.inventory.set(slot,item);}
            int itemCount=p.inventory.size;h.held=null;h.pos.set(WristHotbar.slotCenter(Vr.hands[0].world,slots,slot));
            check(Vr.takeHotbar()&&h.held==item,"Wrist quick slot did not select real inventory item");
            check(p.inventory.size==itemCount&&p.inventory.contains(item,true),"Hotbar duplicated or consumed item");
            Vr.release(1);check(h.held==null&&p.inventory.contains(item,true),"Release at wrist dropped the item");
            h.pos.set(new Vector3((slots*.5f+.5f)*WristHotbar.CELL,0,0).mul(WristHotbar.frame(Vr.hands[0].world)));
            boolean menuWasOpen=Vr.current.getShowingMenu();
            Vr.pToggleInventory();check(Vr.current.getShowingMenu()!=menuWasOpen,"Y inventory toggle failed");
            check(com.interrupt.dungeoneer.game.Game.inventoryScreen.isVisible(),"Stock inventory screen not shown");
            boolean equipment=false;
            for(com.interrupt.dungeoneer.ui.EquipLoc loc:com.interrupt.dungeoneer.game.Game.hud.equipLocations.values())if(loc.visible)equipment=true;
            check(equipment,"Equipment slots were not restored");
            testNativePointer();
            Vr.renderScene(Vr.renderer,Vr.current);
            Vr.nativeUiFrame.begin();com.badlogic.gdx.graphics.Pixmap uiPixels=com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixmap(0,0,8,Vr.nativeUiFrame.getHeight());Vr.nativeUiFrame.end();
            check((uiPixels.getPixel(1,uiPixels.getHeight()/2)&255)==0,"Inventory dimmed the whole UI canvas");uiPixels.dispose();
            Vr.pToggleInventory();
            Vr.togglePause();check(com.interrupt.dungeoneer.overlays.OverlayManager.instance.current() instanceof com.interrupt.dungeoneer.overlays.PauseOverlay,"B did not open pause from gameplay");
            Vr.togglePause();com.interrupt.dungeoneer.overlays.OverlayManager.instance.draw(1f);check(!(com.interrupt.dungeoneer.overlays.OverlayManager.instance.current() instanceof com.interrupt.dungeoneer.overlays.PauseOverlay),"B did not dismiss pause");
            Vr.pToggleInventory();Vr.pToggleInventory();check(!Vr.current.getShowingMenu()&&!(com.interrupt.dungeoneer.overlays.OverlayManager.instance.current() instanceof com.interrupt.dungeoneer.overlays.PauseOverlay),"Y closing inventory opened pause");
            if(itemIndex!=slot){p.inventory.set(slot,displaced);p.inventory.set(itemIndex,item);}
            h.pos.set(previousPos);Vr.hands[0].world.set(leftWorld);Vr.hands[0].valid=leftValid;
            // Release with a fast hand must execute Delver's toss hook, not just
            // place an item on the ground or store it near the chest.
            p.addToInventory(throwable,false);h.held=throwable;h.pos.set(p.x,p.z+p.eyeHeight,p.y);h.throwVelocity.set(2,.5f,0);
            boolean leftWasValid=Vr.hands[0].valid;Vr.hands[0].valid=false;
            Vr.release(1);Vr.hands[0].valid=leftWasValid;
            check(throwable.tosses==1&&throwable.power>0&&!p.inventory.contains(throwable,true)&&throwable.isActive,"VR release skipped native throwing mechanics");
            check(throwable.xa>0&&throwable.za>0,"Hand release velocity not applied to throw");
            String shader="uniform mat4 u_projectionViewMatrix; void main(){ vec4 newPosition=a_position; float d=length(u_LightPositions[i] - gl_Position.xyz); }";
            String rewritten=StereoLighting.vertex(shader);
            check(rewritten.contains("u_LightPositions[i] - (u_vrModelMatrix * newPosition).xyz")&&!rewritten.contains("- gl_Position.xyz"),"Animated lighting still depends on eye projection");
            check(StereoLighting.vertex("uniform mat4 u_projectionViewMatrix; u_LightPositions[i] - gl_Position.xyz").contains("- (u_vrModelMatrix * a_position).xyz"),"World lighting shader was not rewritten");
            Vector2 heldSize=HeldVisual.size(16,32,.26f);check(Math.abs(heldSize.x/heldSize.y-.5f)<.001f,"Held sprite aspect ratio stretched");
            for(Item candidate:p.inventory)if(candidate!=null&&HeldVisual.blade(candidate)){
                Vector3 blade=HeldVisual.tip(new Matrix4(),candidate).nor();
                check(blade.dot(new Vector3(0,0,-1))>.999f,"Visible blade and forward collider are not aligned");break;
            }
            check(HeldVisual.center(new Matrix4(),item).z<-.10f,"Ordinary item remains sunk into palm");
            com.interrupt.dungeoneer.entities.items.Bow bow=new com.interrupt.dungeoneer.entities.items.Bow();
            Matrix4 bowHand=new Matrix4().translate(.3f,.8f,-.5f).rotate(Vector3.Z,35);
            Matrix4 bowPose=HeldVisual.orientation(bowHand,bow);
            Vector3 bowGrip=new Vector3(-HeldVisual.dimensions(bow).x*.32f/Vr.SCALE,0,0).rot(bowPose).add(HeldVisual.center(bowHand,bow));
            check(bowGrip.dst(new Vector3(0,0,-.035f).mul(bowHand))<.00001f,"Bow handle does not meet the hand grip");
            check(new Vector3(1,0,0).rot(bowPose).dot(new Vector3(0,0,1).rot(bowHand))>.999f,"Bow string does not face toward the player");
            com.interrupt.dungeoneer.gfx.DynamicLight red=com.interrupt.dungeoneer.gfx.GlRenderer.getLight();
            red.position.set(h.pos);red.color.set(1,0,0);red.range=2;
            com.interrupt.dungeoneer.gfx.DynamicLight blue=com.interrupt.dungeoneer.gfx.GlRenderer.getLight();
            blue.position.set(h.pos).add(.1f,0,0);blue.color.set(0,0,1);blue.range=2;
            // Regression mode compares both eyes' clocks, world light positions
            // and colors after the real renderer runs with these colored lights.
            Vr.renderScene(Vr.renderer,Vr.current);
            Vr.buffers[0].begin();java.nio.IntBuffer stencil=org.lwjgl.BufferUtils.createIntBuffer(1);com.badlogic.gdx.Gdx.gl.glGetIntegerv(com.badlogic.gdx.graphics.GL20.GL_STENCIL_BITS,stencil);Vr.buffers[0].end();
            check(stencil.get(0)>=8,"Eye target lacks stencil storage for lighting halos");
            int before=com.interrupt.dungeoneer.gfx.GlRenderer.usedLights;
            com.interrupt.dungeoneer.entities.DynamicLight offscreen=new com.interrupt.dungeoneer.entities.DynamicLight();
            offscreen.x=1000;offscreen.y=1000;offscreen.z=1000;offscreen.isActive=true;
            offscreen.tick(level,1);
            check(com.interrupt.dungeoneer.gfx.GlRenderer.usedLights>before,"Offscreen emitter was culled by a single-eye frustum");
            check(Vr.renderer.mapTextureRegion!=null,"Explored wrist map texture was not prepared");
            Vr.wristStrategy.beforeGroups();check(com.badlogic.gdx.Gdx.gl.glIsEnabled(com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST),"Wrist UI is still an x-ray overlay");Vr.wristStrategy.afterGroups();
            check(com.badlogic.gdx.Gdx.gl.glIsEnabled(com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST),"Wrist UI did not restore scene depth state");
            testWristOcclusion();
            testPostProcessing();
            testSteps();
            float savedOffset=Vr.stepSmoothing.offset,savedVelocity=Vr.stepSmoothing.velocity;
            float savedReported=Vr.reportedRise,savedBody=Vr.lastBodyZ,savedZ=p.z,savedZa=p.za,savedDt=Vr.dt;
            boolean savedFloating=p.floating,savedLadder=p.isOnLadder;
            Actor actor=p;Float savedTimer=actor.stepUpTimer,savedLerp=actor.stepUpLerp;
            try{
                Vr.stepSmoothing.reset();float bodyZ=p.z;
                p.stepUp(.3f);
                check(Math.abs(Vr.stepSmoothing.offset+.3f)<.00001f,"Native step event did not reach VR comfort smoothing");
                check(p.z==bodyZ,"Visual smoothing moved the physics body");
                check(Locomotion.speed(p)>.8f,"Full stick still uses the old fixed speed cap");
                Vr.stepSmoothing.reset();Vr.reportedRise=0;Vr.lastBodyZ=p.z;Vr.dt=0;p.za=0;p.floating=false;p.isOnLadder=false;
                p.z+=.025f;Vr.finishHeight(p);
                check(Math.abs(Vr.stepSmoothing.offset+.025f)<.00001f,"Small unreported ledge bypassed smoothing");
                p.stepUp(.12f);p.z+=.12f;Vr.finishHeight(p);
                check(Math.abs(Vr.stepSmoothing.offset+.145f)<.00001f,"Reported step was smoothed twice");
                p.z+=.2f;Vr.finishHeight(p);
                check(Math.abs(Vr.stepSmoothing.offset+.345f)<.00001f,"Floor height change bypassed smoothing");
                p.z+=5;Vr.finishHeight(p);check(Vr.stepSmoothing.offset==0,"Teleport retained a step offset");
            }finally{Vr.stepSmoothing.offset=savedOffset;Vr.stepSmoothing.velocity=savedVelocity;actor.stepUpTimer=savedTimer;actor.stepUpLerp=savedLerp;Vr.reportedRise=savedReported;Vr.lastBodyZ=savedBody;p.z=savedZ;p.za=savedZa;Vr.dt=savedDt;p.floating=savedFloating;p.isOnLadder=savedLadder;}
            for(com.badlogic.gdx.graphics.Pixmap tile:new com.badlogic.gdx.graphics.Pixmap[]{HandTextures.skin(),HandTextures.sleeve()}){
                java.util.Set<Integer> palette=new java.util.HashSet<Integer>();
                for(int y=0;y<tile.getHeight();y++)for(int x=0;x<tile.getWidth();x++){int pixel=tile.getPixel(x,y);check((pixel&255)==255,"Hand texture contains transparent holes");palette.add(pixel);}
                check(palette.size()>=5&&palette.size()<=10,"Hand texture is flat or no longer uses a limited pixel palette");tile.dispose();
            }
            System.out.println("INTERACTION PASS: door, NPC trigger, moving-item pickup, stereo lighting, wrist transforms, hotbar selection and storage without consumption.");
        }finally{
            p.removeFromInventory(item);p.selectedBarItem=previousSelected;p.heldItem=previousHeldSlot;
            p.removeFromInventory(throwable);level.entities.removeValue(throwable,true);level.non_collidable_entities.removeValue(throwable,true);h.throwVelocity.set(oldThrow);
            h.world.set(previousWorld);h.pos.set(previousPos);h.held=previousHeld;Vr.interactionEntities=previous;
            Vr.hands[0].world.set(leftWorld);Vr.hands[0].valid=leftValid;
        }
    }
    static void testPostProcessing(){
        com.interrupt.dungeoneer.game.Options o=com.interrupt.dungeoneer.game.Options.instance;
        boolean enabled=o.enablePostProcessing,fxaa=o.fxaaEnabled;int quality=o.postProcessingQuality;String filter=o.postProcessFilter;
        PostProcessing post=new PostProcessing();
        com.badlogic.gdx.graphics.glutils.FrameBuffer target=new com.badlogic.gdx.graphics.glutils.FrameBuffer(com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888,64,64,false);
        try{
            o.enablePostProcessing=true;o.postProcessingQuality=3;
            for(String name:new String[]{"post_filter_Retro","post_filter_Fancy","post_filter_Underworld"}){
                o.postProcessFilter=name;o.fxaaEnabled=false;
                for(int eye=0;eye<2;eye++){
                    target.begin();com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_SCISSOR_TEST);
                    com.badlogic.gdx.Gdx.gl.glClearColor(.31f,.46f,.71f,1);com.badlogic.gdx.Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT);target.end();
                    int before=post.passes;post.apply(target,eye);
                    check(post.passes==before+1,"Native filter not applied: "+name+" eye "+eye);
                    target.begin();java.nio.ByteBuffer pixel=com.badlogic.gdx.utils.BufferUtils.newByteBuffer(4);
                    com.badlogic.gdx.Gdx.gl.glReadPixels(32,32,1,1,com.badlogic.gdx.graphics.GL20.GL_RGBA,com.badlogic.gdx.graphics.GL20.GL_UNSIGNED_BYTE,pixel);target.end();
                    check((pixel.get(3)&255)>240,"Postprocessing lost eye image alpha");
                    if(name.endsWith("Retro"))check(Math.abs((pixel.get(0)&255)-79)>2,"Retro shader did not change rendered pixels");
                    check(com.badlogic.gdx.Gdx.gl.glGetError()==0,"Native filter generated GL error: "+name);
                }
            }
            o.fxaaEnabled=true;post.apply(target,0);check(com.badlogic.gdx.Gdx.gl.glGetError()==0,"FXAA after native filter failed");
            System.out.println("POSTPROCESS PASS: Retro, Fancy, Underworld on both eyes, GPU pixel change and FXAA.");
        }finally{post.dispose();target.dispose();o.enablePostProcessing=enabled;o.fxaaEnabled=fxaa;o.postProcessingQuality=quality;o.postProcessFilter=filter;}
    }
    static void testNativePointer(){
        final int[] events=new int[3];com.badlogic.gdx.InputProcessor saved=com.badlogic.gdx.Gdx.input.getInputProcessor();
        Vr.Hand right=Vr.hands[1];Matrix4 pose=new Matrix4(right.local);boolean valid=right.valid,pressed=right.trigger,previous=right.oldTrigger;
        com.badlogic.gdx.Gdx.input.setInputProcessor(new com.badlogic.gdx.InputAdapter(){
            public boolean touchDown(int x,int y,int p,int b){events[0]++;return true;}
            public boolean touchDragged(int x,int y,int p){events[1]++;return true;}
            public boolean touchUp(int x,int y,int p,int b){events[2]++;return true;}
        });
        try{
            right.valid=true;right.local.set(NativeUi.plane).translate(0,0,.4f);right.trigger=true;right.oldTrigger=false;
            NativeUi.update();check(NativeUi.down&&com.badlogic.gdx.Gdx.input.isButtonPressed(0),"Native UI did not see trigger as held mouse");
            check(Math.abs(com.badlogic.gdx.Gdx.input.getX()-com.badlogic.gdx.Gdx.graphics.getWidth()/2)<2,"VR ray/polled cursor disagree");
            right.oldTrigger=true;right.local.translate(.1f,0,0);NativeUi.update();right.trigger=false;NativeUi.update();
            check(events[0]==1&&events[1]>=1&&events[2]==1&&!NativeUi.down,"Native drag press/move/release sequence broken");
        }finally{right.local.set(pose);right.valid=valid;right.trigger=pressed;right.oldTrigger=previous;com.badlogic.gdx.Gdx.input.setInputProcessor(saved);}
    }
    static void testWristOcclusion(){
        com.badlogic.gdx.graphics.glutils.FrameBuffer target=new com.badlogic.gdx.graphics.glutils.FrameBuffer(com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888,32,32,true);
        com.badlogic.gdx.graphics.PerspectiveCamera camera=new com.badlogic.gdx.graphics.PerspectiveCamera(60,32,32);camera.position.set(0,0,3);camera.lookAt(0,0,0);camera.near=.1f;camera.far=10;camera.update();
        WristGroupStrategy strategy=new WristGroupStrategy(camera);com.badlogic.gdx.graphics.g3d.decals.DecalBatch batch=new com.badlogic.gdx.graphics.g3d.decals.DecalBatch(strategy);
        try{
            for(int pass=0;pass<2;pass++){
                target.begin();com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_CULL_FACE);com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST);com.badlogic.gdx.Gdx.gl.glDepthMask(true);com.badlogic.gdx.Gdx.gl.glClearColor(0,0,0,1);com.badlogic.gdx.Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT|com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT);
                Vr.shapes.setProjectionMatrix(camera.combined);Vr.shapes.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);Vr.shapes.setColor(com.badlogic.gdx.graphics.Color.BLUE);Vr.shapes.rect(-1,-1,2,2);Vr.shapes.end();
                com.badlogic.gdx.graphics.g3d.decals.Decal panel=com.badlogic.gdx.graphics.g3d.decals.Decal.newDecal(1,1,new com.badlogic.gdx.graphics.g2d.TextureRegion(Vr.white),true);panel.setColor(com.badlogic.gdx.graphics.Color.RED);panel.setPosition(0,0,pass==0?-1:1);batch.add(panel);batch.flush();
                com.badlogic.gdx.graphics.Pixmap pixel=com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixmap(16,16,1,1);int color=pixel.getPixel(0,0);pixel.dispose();target.end();
                check(pass==0?(color&0x0000ff00)!=0:(color&0xff000000)!=0,pass==0?"Wrist panel rendered through opaque geometry":"Visible wrist panel was lost behind depth");
            }
        }finally{batch.dispose();strategy.dispose();target.dispose();}
    }
    static void testSteps(){
        Player walker=new Player();walker.isSolid=true;walker.z=.5f;
        StepLevel fixture=new StepLevel();fixture.rise=.30f;
        Locomotion.move(fixture,walker,.02f,0);check(walker.x>.01f&&fixture.receivedStep>.30f,"VR used Entity.stepHeight instead of native Player step height");
        walker.x=0;fixture.rise=.5f;Locomotion.move(fixture,walker,.02f,0);check(walker.x==0,"VR climbed an over-height world ledge");
        fixture.rise=0;fixture.obstacle=new Entity();fixture.obstacle.z=.5f;fixture.obstacle.collision.z=.2f;fixture.obstacle.canStepUpOn=true;
        Locomotion.move(fixture,walker,.02f,0);check(walker.x>.01f&&Math.abs(walker.z-.7f)<.001f,"Low step-enabled prop blocked VR movement");
        walker.x=0;walker.z=.5f;fixture.headroom=false;Locomotion.move(fixture,walker,.02f,0);check(walker.x==0&&walker.z==.5f,"VR stepped into a low ceiling");
        fixture.headroom=true;fixture.obstacle.canStepUpOn=false;Locomotion.move(fixture,walker,.02f,0);check(walker.x==0,"VR climbed a non-stepable object");
        fixture.obstacle.canStepUpOn=true;Locomotion.move(fixture,walker,0,.02f);check(walker.y>.01f&&Math.abs(walker.z-.7f)<.001f,"Y-axis step differs from X-axis step");
        System.out.println("STEP PASS: native player step height, world ledges, low props, tall ledges, ceilings and both movement axes.");
    }
}






