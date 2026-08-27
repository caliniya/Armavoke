package caliniya.vergvoke.ui.windows;

import arc.scene.ui.layout.Table;
import caliniya.vergvoke.campaign.TechNode;
import caliniya.vergvoke.campaign.TechTree;
import caliniya.vergvoke.content.Techs;
import caliniya.vergvoke.ui.Button;

/** 科技树测试窗口：以树形结构显示节点与解锁/研究状态，可点击研究。 */
public class TechTreeWindow extends Window {

  public TechTreeWindow() {
    super("科技树");
  }

  @Override
  public void main(Table t) {
    t.clearChildren();

    TechTree tree = Techs.tree;
    if (tree == null || tree.nodes.size == 0) {
      t.add("[red]科技树为空（Techs.load 没执行？）[]").row();
      return;
    }

    t.add("[lightgray]共 " + tree.nodes.size + " 个节点[]").left().row();
    t.add().height(8f).row();

    // 从每个根节点开始递归绘制整棵子树
    for (TechNode node : tree.nodes) {
      if (node.parent == null) {
        drawNode(t, node, 0, true);
      }
    }
  }

  /**
   * 递归绘制一个节点及其子树。
   *
   * @param depth 深度（用于缩进/树枝符号）
   * @param last  是否为父节点的最后一个子节点（决定用 └─ 还是 ├─）
   */
  private void drawNode(Table t, TechNode node, int depth, boolean last) {
    // 构建树形前缀
    StringBuilder prefix = new StringBuilder();
    for (int i = 1; i < depth; i++) {
      prefix.append("  ");
    }
    if (depth > 0) {
      prefix.append(last ? "└─ " : "├─ ");
    }

    String status =
        (node.unlocked ? "[green]已解锁[]" : "[red]未解锁[]")
            + " "
            + (node.researched ? "[green]已研究[]" : "[red]未研究[]");

    Table row = new Table();
    row.defaults().pad(2f);
    row.add(prefix + node.name).expandX().left();
    row.add(status).width(140f).left();
    row.add(
            new Button(
                "研究",
                () -> {
                  Techs.tree.research(node.name);
                  main(this.main); // 刷新
                }))
        .size(80f, 42f);
    t.add(row).growX();
    t.row();

    for (int i = 0; i < node.children.size; i++) {
      drawNode(t, node.children.get(i), depth + 1, i == node.children.size - 1);
    }
  }
}
