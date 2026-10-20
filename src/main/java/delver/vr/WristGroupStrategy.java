package delver.vr;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;

/** World-occluded wrist panels with a small surface bias, never an x-ray overlay. */
public final class WristGroupStrategy extends CameraGroupStrategy {
    public WristGroupStrategy(Camera camera){super(camera);}
    @Override public void beforeGroups(){super.beforeGroups();Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);Gdx.gl.glDepthFunc(GL20.GL_LEQUAL);Gdx.gl.glDepthMask(false);Gdx.gl.glEnable(GL20.GL_POLYGON_OFFSET_FILL);Gdx.gl.glPolygonOffset(-1,-1);}
    @Override public void afterGroups(){super.afterGroups();Gdx.gl.glDisable(GL20.GL_POLYGON_OFFSET_FILL);Gdx.gl.glDepthMask(true);Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);}
}
