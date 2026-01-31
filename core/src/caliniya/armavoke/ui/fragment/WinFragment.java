package caliniya.armavoke.ui.fragment;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.NinePatch;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.scene.style.NinePatchDrawable;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Align;
import caliniya.armavoke.ui.Button;

public abstract class WinFragment {

  public Table root; // 全屏透明根节点 (用于居中定位)
  public Table main; // 窗口实体 (带背景)
  public Table content; // 给子类用的内容填充区
  
  public float w = 400f, h = 300f; // 默认大小
  public String title = "Window"; // 默认标题

  public void build() {
    // 1. 创建全屏根节点 (用于把窗口居中)
    // touchable = childrenOnly 确保点击窗口外的空白区域会穿透到游戏
    root = new Table();
    root.setFillParent(true);
    root.touchable = Touchable.childrenOnly;
    
    // 2. 创建窗口主体
    main = new Table();
    
    // --- 核心修改：九宫格背景 ---
    // 假设你的 "Window" 图片已经是处理好边缘的素材
    // NinePatch 需要指定 左、右、上、下 的边距 (split values)
    // 如果你在 TexturePacker 里配置了 split，Core.atlas.find 会自动返回 NinePatchDrawable
    // 如果没有配置，这里手动创建 NinePatch。假设边框宽度大概是 12 像素：
    var region = Core.atlas.find("Window");
        // 参数：region, left, right, top, bottom
        // 根据你的素材实际边框厚度调整这4个数字
        int split = 12; 
        main.setBackground(new NinePatchDrawable(new NinePatch(region, split, split, split, split)));
    

    // 设置窗口可触摸，拦截点击事件，防止穿透
    main.touchable = Touchable.enabled;

    // --- 3. 组装窗口内部结构 ---
    
    // 3.1 标题栏 (Title Bar)
    Table titleTable = new Table();
    // 标题文字 (左对齐)
    titleTable.add(new Label(title)).color(Color.white).expandX().left().padLeft(10f);
    
    // 关闭按钮 (右对齐)
    // 这里用个简单的 "X" 按钮，你可以换成图标 Button(Icon.cancel)
    Button closeBtn = new Button("X" ,() -> this.remove()); 
    titleTable.add(closeBtn).size(30f).right().padRight(5f);
    
    // --- 4. 将部分组合到 main ---
    // 添加标题栏，限制高度，填满水平宽度
    main.add(titleTable).growX().height(40f).top(); 
    main.row(); // 换行
    
    // 可选：添加一条分割线
    main.image().color(Color.gray).fillX().height(2f);
    main.row();
    
    // 添加内容区，占据剩余所有空间 (grow())
    main.add(content).grow().pad(10f);

    // --- 5. 最终布局 ---
    // 设置窗口固定大小
    // main.setSize(w, h); // 在 Table 布局中，通常用 size/width/height 约束单元格
    
    // 将 main 添加到 root 中心，并指定大小
    root.add(main).size(w, h).center();
    
    Core.scene.root.addChild(root);
  }
  
  /** 销毁窗口 */
  public void remove() {
      if(root != null) {
          root.remove();
          root = null;
      }
  }
  
  /** 动态修改标题 */
  public void setTitle(String newTitle) {
      this.title = newTitle;
      // 注意：如果窗口已经 build 了，这里改变量不会自动刷新 UI
      // 实际应用中可能需要保留 Label 引用来 setText
  }
}