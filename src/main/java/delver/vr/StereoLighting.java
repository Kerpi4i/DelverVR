package delver.vr;

/** Keep the game's attenuation curve, but measure light distances in world space. */
public final class StereoLighting {
    public static final com.badlogic.gdx.math.Matrix4 model=new com.badlogic.gdx.math.Matrix4();
    public static void apply(com.badlogic.gdx.graphics.glutils.ShaderProgram shader){if(shader.hasUniform("u_vrModelMatrix"))shader.setUniformMatrix("u_vrModelMatrix",model);}
    public static String vertex(String source) {
        if(!source.contains("u_LightPositions")||!source.contains("uniform mat4 u_projectionViewMatrix;"))return source;
        String position=source.contains("vec4 newPosition")?"newPosition":"a_position";
        source=source.replace("uniform mat4 u_projectionViewMatrix;","uniform mat4 u_projectionViewMatrix;\nuniform mat4 u_vrModelMatrix;");
        return source.replaceAll("u_LightPositions\\[i\\]\\s*-\\s*gl_Position\\.xyz", "u_LightPositions[i] - (u_vrModelMatrix * "+position+").xyz");
    }
}
