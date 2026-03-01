package caliniya.armavoke.core;

import arc.graphics.Camera;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.util.viewport.Viewport;
import caliniya.armavoke.ui.fragment.*;

import static arc.Core.scene;
import static arc.Core.graphics;

public class UI {
  
  // 缓存 Scl 值，避免重复计算
  public static float scl;
  
  //调试显示器
  public static DebugFragment debug;
  //主游戏ui
  public static GameFragment game;
  //游戏菜单ui
  public static MenuFragment menu;
  //地图列表
  public static MapsFragment maps;
  //ui用的相机和视口
  public static Camera camera;
  public static Viewport vport;
  
  private static boolean isDebugShown = true;
  
  public static void initAll(){
    // 初始化 Scl 缓存
    scl = Scl.scl();
    
    if(debug == null) {
      debug = new DebugFragment();
    }
    if(game == null) {
      game = new GameFragment();
    }
    if(menu == null) {
      menu = new MenuFragment();
    }
    if(maps == null) {
      maps = new MapsFragment();
    }
    camera = scene.getCamera();
  }

  public static void Menu() {
    scene.clear();
    menu.build();
    Debug();
  }

  // 加载界面渲染逻辑
  public static void Loading(float progress) {
    float screenW = graphics.getWidth();
    float screenH = graphics.getHeight();
    float centerX = screenW / 2f;
    float centerY = screenH / 2f;

    float barWidth = 300f; 
    float barHeight = 20f; 
    float padding = 4f; 

    Draw.color(Color.white);
    Lines.stroke(2f);
    Lines.rect(centerX - barWidth / 2f, centerY - barHeight / 2f, barWidth, barHeight);
    float maxFillWidth = barWidth - padding * 2;
    float currentFillWidth = maxFillWidth * progress;
    float fillHeight = barHeight - padding * 2;
    float leftEdgeX = centerX - barWidth / 2f + padding;
    float drawCenterX = leftEdgeX + currentFillWidth / 2f;
    Fill.rect(drawCenterX, centerY, currentFillWidth, fillHeight);
    Draw.flush();
  }

  public static void Game() {
    scene.clear();
    game.build();
    Debug();
  }
  
  public static void Maps() {
    maps.build();
  }
  
  /**
   * 创建一个窗口
   * @param title 窗口标题
   * @param widthRatio 窗口宽度占屏幕宽度的比例 (0~1)
   * @param heightRatio 窗口高度占屏幕高度的比例 (0~1)
   */
  public static void Window(String Ttitle, float widthRatio, float heightRatio) {
    // 计算实际像素尺寸：屏幕尺寸 * 比例 / Scl缩放因子
    // 除以 scl 是因为 Cell.size() 内部会乘以 scl
    float actualW = graphics.getWidth() * widthRatio / scl;
    float actualH = graphics.getHeight() * heightRatio / scl;
    
    WinFragment win = new WinFragment(){{
      title = Ttitle;
      w = actualW;
      h = actualH;
    }};
    win.build();
  }

  public static void Debug() {
    if (isDebugShown) {
      debug.add();
    }
  }
}