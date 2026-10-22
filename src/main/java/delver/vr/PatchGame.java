package delver.vr;

import javassist.*;
import javassist.expr.*;
import java.nio.file.*;

/** Produces only patched classes; the user's Steam archive is never modified. */
public final class PatchGame {
    public static void main(String[] args) throws Exception {
        ClassPool cp = ClassPool.getDefault();
        cp.insertClassPath(args[0]);
        cp.insertClassPath(args[1]);
        CtClass options=cp.get("com.interrupt.dungeoneer.overlays.OptionsOverlay");
        options.getDeclaredMethod("makeContent").insertAfter("delver.vr.VrOptionsOverlay.add(this,$_);");
        options.writeFile(args[2]);
        CtClass mods=cp.get("com.interrupt.dungeoneer.game.ModManager");
        mods.getDeclaredMethod("findMods").insertAfter("delver.vr.Workshop.append(this.allMods);");mods.writeFile(args[2]);
        CtClass actor=cp.get("com.interrupt.dungeoneer.entities.Actor");
        actor.getDeclaredMethod("stepUp").insertBefore("delver.vr.Vr.onStep(this,$1);");
        actor.writeFile(args[2]);
        CtClass player=cp.get("com.interrupt.dungeoneer.entities.Player");
        player.getDeclaredMethod("UseInventoryItem").insertBefore("if(delver.vr.NativeUi.useConsumable(this,$1)) return;");
        player.getDeclaredMethod("tick",new CtClass[]{cp.get("com.interrupt.dungeoneer.game.Level"),CtClass.floatType}).instrument(new ExprEditor(){
            public void edit(FieldAccess f)throws CannotCompileException{
                if(f.isWriter()&&f.getFieldName().equals("isOnLadder"))
                    f.replace("{ if(!$1) delver.vr.Vr.onLadder(this); $proceed($$); }");
            }
        });
        player.writeFile(args[2]);
        // Android has no captured desktop cursor: its backend already reports false.
        // GameInput must still leave its initial caughtCursor=true state on inventory open.
        CtClass input=cp.get("com.interrupt.dungeoneer.GameInput");
        input.getDeclaredMethod("setCursorCatched").insertBefore("this.caughtCursor=$1;");
        input.writeFile(args[2]);
        CtClass renderer = cp.get("com.interrupt.dungeoneer.gfx.GlRenderer");
        CtMethod original = renderer.getDeclaredMethod("render");
        original.setName("vrRenderOriginal");
        original.instrument(new ExprEditor() {
            public void edit(FieldAccess f)throws CannotCompileException {
                if(f.isWriter()&&f.getFieldName().equals("time")&&f.getClassName().equals(renderer.getName()))
                    f.replace("{ if(!delver.vr.Vr.secondEye()) $proceed($$); }");
            }
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("update") && m.getClassName().equals("com.badlogic.gdx.graphics.PerspectiveCamera"))
                    m.replace("{ $proceed($$); delver.vr.Vr.camera($0); }");
                if (m.getMethodName().equals("drawHeldItem") || m.getMethodName().equals("drawOffhandItem") || m.getMethodName().equals("drawUI"))
                    m.replace("{}");
            }
        });
        renderer.addMethod(CtNewMethod.make("public void render(com.interrupt.dungeoneer.game.Game g) { delver.vr.Vr.renderScene(this,g); }", renderer));
        renderer.addMethod(CtNewMethod.make("public void vrDrawNativeUi() { "
            +"com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_CULL_FACE); uiBatch.setProjectionMatrix(camera2D.combined); "
            +"com.interrupt.dungeoneer.game.Game.hudManager.backpack.yOffset=1.7f; "
            +"drawInventory(com.interrupt.dungeoneer.game.Game.hudManager.quickSlots); drawInventory(com.interrupt.dungeoneer.game.Game.hudManager.backpack); drawInventory(com.interrupt.dungeoneer.game.Game.hud.equipLocations); "
            +"com.interrupt.dungeoneer.game.Game.ui.draw(); "
            +"com.interrupt.dungeoneer.entities.Item hovered=com.interrupt.dungeoneer.game.Game.hud.getMouseOverItem(); "
            +"if(hovered!=null){ uiBatch.begin(); com.interrupt.dungeoneer.game.Game.tooltip.show((float)com.badlogic.gdx.Gdx.input.getX(),(float)(com.badlogic.gdx.Gdx.graphics.getHeight()-com.badlogic.gdx.Gdx.input.getY()),hovered);uiBatch.end(); } else com.interrupt.dungeoneer.game.Game.tooltip.hide(); }",renderer));
        renderer.getDeclaredMethod("startFrame").insertBefore("delver.vr.Vr.bindEye(); com.badlogic.gdx.Gdx.gl.glDepthMask(true);");
        // The desktop mirror disables depth after the stereo pair, whereas hands
        // leave it enabled between eyes. Sky planes must not inherit either state.
        renderer.getDeclaredMethod("renderSkybox").insertBefore("com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST); com.badlogic.gdx.Gdx.gl.glDepthMask(false);");
        renderer.getDeclaredMethod("renderSkybox").insertAfter("com.badlogic.gdx.Gdx.gl.glDepthMask(true); com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST); com.badlogic.gdx.Gdx.gl.glDepthFunc(com.badlogic.gdx.graphics.GL20.GL_LEQUAL);",true);
        renderer.getDeclaredMethod("renderStencilPass").insertBefore("$1=delver.vr.ShadowBatch.prepare($1);");
        renderer.getDeclaredMethod("Render").instrument(new ExprEditor(){
            public void edit(MethodCall m)throws CannotCompileException {
                if(m.getMethodName().equals("updateDrawable"))
                    m.replace("{ delver.vr.RenderPreparation.update($0); }");
            }
        });
        renderer.getDeclaredMethod("endFrame").insertBefore("delver.vr.Vr.drawHands();");
        // The stereo compositor owns postprocessing; never bind a desktop canvas.
        for(String method:new String[]{"startFrame","endFrame"})renderer.getDeclaredMethod(method).instrument(new ExprEditor(){
            public void edit(FieldAccess f)throws CannotCompileException{
                if(f.isReader()&&f.getFieldName().equals("enablePostProcessing"))f.replace("{ $_=false; }");
            }
        });
        renderer.getDeclaredMethod("renderDrawableSprite").instrument(new ExprEditor(){
            public void edit(MethodCall m)throws CannotCompileException {
                if(m.getClassName().equals("com.badlogic.gdx.graphics.Color")&&m.getMethodName().equals("lerp")&&m.getSignature().startsWith("(FFFFF)"))
                    m.replace("{ if(delver.vr.Vr.secondEye()) $5=0f; $_=$proceed($$); }");
            }
        });
        renderer.getDeclaredMethod("updateDynamicLights").setBody("{"+
            "if(!delver.vr.Vr.secondEye()) { delver.vr.LightSelection.sort(usedLightPool,$1.position); }"+
            "java.util.Arrays.fill(lightColors,0f); java.util.Arrays.fill(lightPositions,0f);"+
            "for(int n=0;n<usedLightPool.size && n<MAX_DYNAMIC_LIGHTS;n++){ com.interrupt.dungeoneer.gfx.DynamicLight l=(com.interrupt.dungeoneer.gfx.DynamicLight)usedLightPool.get(n);"+
            "lightPositions[n*3]=l.position.x;lightPositions[n*3+1]=l.position.y;lightPositions[n*3+2]=l.position.z;"+
            "lightColors[n*4]=l.color.x;lightColors[n*4+1]=l.color.y;lightColors[n*4+2]=l.color.z;lightColors[n*4+3]=l.range*0.3333f; }}");
        renderer.writeFile(args[2]);
        CtClass shader=cp.get("com.badlogic.gdx.graphics.glutils.ShaderProgram");
        shader.getDeclaredConstructor(new CtClass[]{cp.get("java.lang.String"),cp.get("java.lang.String")}).insertBeforeBody("$1=delver.vr.StereoLighting.vertex($1);");
        shader.getDeclaredMethod("begin").insertAfter("delver.vr.StereoLighting.apply(this);");
        shader.writeFile(args[2]);
        CtClass info=cp.get("com.interrupt.dungeoneer.gfx.shaders.ShaderInfo");
        info.getDeclaredMethod("begin",new CtClass[]{CtClass.intType}).insertAfter("delver.vr.StereoLighting.apply(this.shader);");
        info.writeFile(args[2]);
        CtClass mesh=cp.get("com.interrupt.dungeoneer.gfx.drawables.DrawableMesh");
        mesh.getDeclaredMethod("draw").instrument(new ExprEditor(){
            public void edit(MethodCall m)throws CannotCompileException {
                if(m.getMethodName().equals("setAttributes"))m.replace("{ delver.vr.StereoLighting.model.set(this.modelView); $proceed($$); }");
            }
        });
        mesh.getDeclaredMethod("draw").insertAfter("delver.vr.StereoLighting.model.idt();",true);
        mesh.writeFile(args[2]);
        CtClass emitter=cp.get("com.interrupt.dungeoneer.entities.DynamicLight");
        emitter.getDeclaredMethod("tick").instrument(new ExprEditor(){
            public void edit(MethodCall m)throws CannotCompileException {
                if(m.getMethodName().startsWith("sphereInFrustum"))m.replace("{ $_=true; }");
            }
        });
        emitter.writeFile(args[2]);
        CtClass weapon=cp.get("com.interrupt.dungeoneer.entities.items.Weapon");
        weapon.getDeclaredMethod("getCrosshairDirection").insertBefore("if(delver.vr.Vr.shooting) return new com.badlogic.gdx.math.Vector3(delver.vr.Vr.shotDirection);");
        weapon.writeFile(args[2]);
        CtClass bow=cp.get("com.interrupt.dungeoneer.entities.items.Bow");
        bow.getDeclaredMethod("setMissileDirectionAndPosition").insertAfter("if(delver.vr.Vr.shooting) { float v=(float)Math.sqrt((double)($1.xa*$1.xa+$1.ya*$1.ya+$1.za*$1.za)); $1.x=delver.vr.Vr.shotPosition.x; $1.y=delver.vr.Vr.shotPosition.z; $1.z=delver.vr.Vr.shotPosition.y+0.5f; $1.xa=delver.vr.Vr.shotDirection.x*v; $1.ya=delver.vr.Vr.shotDirection.z*v; $1.za=delver.vr.Vr.shotDirection.y*v; }");
        bow.writeFile(args[2]);
        System.out.println("Patched renderer: stereo dispatch, projection, eye buffers, tracked hands.");
    }
}

