package caliniya.armavoke.system.input;

import arc.Core;
import arc.input.GestureDetector.GestureListener;
import arc.input.InputProcessor;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import caliniya.armavoke.core.Render;
import caliniya.armavoke.system.BasicSystem;

@SuppressWarnings("unused")
public class CameraInput extends BasicSystem<CameraInput>
    implements GestureListener, InputProcessor {

  private boolean up, down, left, right;
  private float keySpeed = 10f;
  private float lastZoomSnapshot = 1f;

  @Override
  public CameraInput init() {
    this.index = 1;
    return super.init();
  }

  @Override
  public void update() {
    if (!inited) return;

    float currentZoom = Render.currentZoom;
    float speed = keySpeed * currentZoom * (Core.input.keyDown(KeyCode.shiftLeft) ? 2f : 1f);

    if (up) Core.camera.position.y += speed;
    if (down) Core.camera.position.y -= speed;
    if (left) Core.camera.position.x -= speed;
    if (right) Core.camera.position.x += speed;
  }

  @Override
  public boolean pan(float x, float y, float deltaX, float deltaY) {

    Core.camera.position.x -= deltaX * Render.currentZoom;
    Core.camera.position.y -= deltaY * Render.currentZoom;
    return false;
  }

  @Override
  public boolean touchDown(float x, float y, int pointer, KeyCode button) {
    lastZoomSnapshot = Render.currentZoom;
    return false;
  }

  @Override
  public boolean zoom(float initialDistance, float distance) {
    if (initialDistance == 0) return false;
    float ratio = initialDistance / distance;
    Render.setZoom(lastZoomSnapshot * ratio);
    return true;
  }

  @Override
  public boolean scrolled(float amountX, float amountY) {
    float zoomSpeed = 0.1f * Render.currentZoom;
    Render.zoom(amountY * zoomSpeed);
    return true;
  }
  
  @Override
  public boolean keyDown(KeyCode key) {
    if (key == KeyCode.w || key == KeyCode.up) up = true;
    if (key == KeyCode.s || key == KeyCode.down) down = true;
    if (key == KeyCode.a || key == KeyCode.left) left = true;
    if (key == KeyCode.d || key == KeyCode.right) right = true;
    return false;
  }

  @Override
  public boolean keyUp(KeyCode key) {
    if (key == KeyCode.w || key == KeyCode.up) up = false;
    if (key == KeyCode.s || key == KeyCode.down) down = false;
    if (key == KeyCode.a || key == KeyCode.left) left = false;
    if (key == KeyCode.d || key == KeyCode.right) right = false;
    return false;
  }

  @Override
  public boolean pinch(Vec2 i1, Vec2 i2, Vec2 p1, Vec2 p2) {
    return false;
  }

  @Override
  public boolean tap(float x, float y, int count, KeyCode button) {
    return false;
  }

  @Override
  public boolean longPress(float x, float y) {
    return false;
  }

  @Override
  public boolean fling(float velocityX, float velocityY, KeyCode button) {
    return false;
  }

  @Override
  public boolean panStop(float x, float y, int pointer, KeyCode button) {
    return false;
  }

  @Override
  public boolean keyTyped(char character) {
    return false;
  }

  @Override
  public boolean touchDown(int screenX, int screenY, int pointer, KeyCode button) {
    return false;
  }

  @Override
  public boolean touchUp(int screenX, int screenY, int pointer, KeyCode button) {
    return false;
  }

  @Override
  public boolean touchDragged(int screenX, int screenY, int pointer) {
    return false;
  }

  @Override
  public boolean mouseMoved(int screenX, int screenY) {
    return false;
  }
}
