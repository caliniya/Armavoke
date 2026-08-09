package caliniya.armavoke.ui.windows;

import arc.scene.ui.layout.Table;
import caliniya.armavoke.campaign.TechNode;
import caliniya.armavoke.campaign.TechTree;
import caliniya.armavoke.content.Techs;
import caliniya.armavoke.ui.Button;

/** 科技树测试窗口：显示所有节点与解锁/研究状态，可点击研究。 */
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

    for (TechNode node : tree.nodes) {
      String status =
          (node.unlocked ? "[green]已解锁[]" : "[red]未解锁[]")
              + " "
              + (node.researched ? "[green]已研究[]" : "[red]未研究[]");

      Table row = new Table();
      row.defaults().pad(3f);
      row.add(node.name).width(170f).left();
      row.add(status).expandX().left();
      row.add(
              new Button(
                  "研究",
                  () -> {
                    tree.research(node.name);
                    main(this.main); // 刷新
                  }))
          .size(84f, 44f);
      t.add(row).growX();
      t.row();
    }
  }
}
