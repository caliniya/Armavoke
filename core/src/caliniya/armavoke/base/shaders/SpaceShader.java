package caliniya.armavoke.base.shaders;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Camera;
import arc.graphics.Texture;
import arc.graphics.Texture.TextureWrap;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.graphics.gl.Shader;
import arc.util.Log;
import arc.util.Time;
import caliniya.armavoke.core.Render;

public class SpaceShader extends Shader {

  private Texture texture;

  // 视差系数
  public float parallaxScale = 0.2f;
  // 基础缩放
  public float baseScale = 0.7f;

  public SpaceShader() {
    super(Core.files.internal("shaders/default.vert"), Core.files.internal("shaders/space.frag"));

    texture = new Texture(Core.files.internal("sprites/space.png"));
    texture.setWrap(TextureWrap.repeat, TextureWrap.repeat);
  }

  @Override
  public void apply() {
    float z = Render.currentZoom * baseScale;

    setUniformf("u_resolution", Core.graphics.getWidth(), Core.graphics.getHeight());
    setUniformf("u_camPos", Core.camera.position.x, Core.camera.position.y);
    setUniformf("u_zoom", z);
    setUniformf("u_texSize", (float) texture.width, (float) texture.height);
    setUniformf("u_parallax", parallaxScale);

    texture.bind(0);
    setUniformi("u_texture", 0);
  }

  /** 使用指定相机和缩放渲染（用于宇宙视图等自定义相机） */
  public void render(Camera cam, float zoom) {
    float z = zoom * baseScale;
    Draw.shader(this);
    setUniformf("u_camPos", cam.position.x, cam.position.y);
    setUniformf("u_zoom", z);
    Draw.rect(Draw.wrap(texture), cam.position.x, cam.position.y, cam.width, cam.height);
    Draw.shader();
  }

  public void render() {
    render(Core.camera, Render.currentZoom);
  }

  @Override
  public void dispose() {
    super.dispose(); // 调用父类释放 shader
    if (texture != null) texture.dispose();
  }
}
