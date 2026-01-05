package caliniya.armavoke.base.shaders;

import arc.Core;
import arc.files.Fi;
import arc.graphics.gl.Shader;
import arc.util.Time;

public class SpaceShader extends Shader {
    
    public SpaceShader() {
        super(
            Core.files.internal("shaders/space.vert"),
            Core.files.internal("shaders/space.frag")
        );
    }

    @Override
    public void apply() {
        // 设置 uniforms
        setUniformf("u_resolution", Core.graphics.getWidth(), Core.graphics.getHeight());
        setUniformf("u_camPos", Core.camera.position.x, Core.camera.position.y);
        setUniformf("u_time", Time.time); // 传递时间
    }
}