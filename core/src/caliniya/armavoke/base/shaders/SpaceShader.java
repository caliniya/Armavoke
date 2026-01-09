package caliniya.armavoke.base.shaders;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Camera;
import arc.graphics.Gl;
import arc.graphics.Texture;
import arc.graphics.Texture.TextureWrap;
import arc.graphics.g2d.Draw;
import arc.graphics.gl.Shader;
import arc.util.Disposable;
import arc.util.Time;
import caliniya.armavoke.core.Render;

public class SpaceShader implements Disposable {
    
    private Shader shader;
    private Texture texture;
    
    // 视差系数
    public float parallaxScale = 0.2f; 
    // 基础缩放
    public float baseScale = 0.7f; 

    public SpaceShader() {
        shader = new Shader(
            Core.files.internal("shaders/default.vert"),
            Core.files.internal("shaders/space.frag")
        );

        texture = new Texture(Core.files.internal("sprites/space.png"));
        
        texture.setWrap(TextureWrap.repeat, TextureWrap.repeat);
    }

    public void render() {
        
        float zoom = Render.currentZoom * baseScale;

        shader.bind();
        
        // 传递参数
        shader.setUniformf("u_resolution", Core.graphics.getWidth(), Core.graphics.getHeight());
        shader.setUniformf("u_camPos", Core.camera.position.x, Core.camera.position.y);
        shader.setUniformf("u_zoom", zoom);
        shader.setUniformf("u_texSize", (float)texture.width, (float)texture.height);
        shader.setUniformf("u_parallax", parallaxScale);
        
        // 绑定纹理
        texture.bind(0);
        shader.setUniformi("u_texture", 0);
        
        Draw.blit(shader);
    }

    @Override
    public void dispose() {
        if (shader != null) shader.dispose();
        if (texture != null) texture.dispose();
    }
}