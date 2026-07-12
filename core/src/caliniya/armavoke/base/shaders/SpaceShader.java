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
import arc.util.Log;
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
    shader =
        new Shader(
            Core.files.internal("shaders/default.vert"), Core.files.internal("shaders/space.frag"));

    texture = new Texture(Core.files.internal("sprites/space.png"));

    texture.setWrap(TextureWrap.repeat, TextureWrap.repeat);
  }

  /** 使用指定相机和缩放渲染（用于宇宙视图等自定义相机） */
  public void render(Camera cam, float zoom) {
    float z = zoom * baseScale;

    shader.bind();
    shader.setUniformf("u_resolution", Core.graphics.getWidth(), Core.graphics.getHeight());
    shader.setUniformf("u_camPos", cam.position.x, cam.position.y);
    shader.setUniformf("u_zoom", z);
    shader.setUniformf("u_texSize", (float) texture.width, (float) texture.height);
    shader.setUniformf("u_parallax", parallaxScale);

    texture.bind(0);
    shader.setUniformi("u_texture", 0);

    Draw.blit(shader);
  }

  public void render() {
    render(Core.camera, Render.currentZoom);
  }

  @Override
  public void dispose() {
    if (shader != null) shader.dispose();
    if (texture != null) texture.dispose();
  }
}
