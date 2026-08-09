package caliniya.armavoke.content;

import caliniya.armavoke.campaign.TechNode;
import caliniya.armavoke.campaign.TechTree;

/**
 * 科技树定义（当前为测试用，使用现有内容）。
 *
 * <p>结构：
 *
 * <pre>
 * base（根）
 * ├── turret   （研究后解锁 Blocks.testTurret）
 * │   ├── building（Blocks.TestBlock，需要先研究 turret）
 * │   └── unit     （UnitTypes.test，需要先研究 turret）
 * </pre>
 */
public class Techs {

  public static TechTree tree;

  public static void load() {
    tree = new TechTree();

    TechNode base = tree.add("base");

    TechNode turret = tree.addChild(base, Blocks.testTurret.techName());
    TechNode building = tree.addChild(turret, Blocks.TestBlock.techName());
    TechNode unit = tree.addChild(turret, UnitTypes.test.techName());

    tree.updateUnlocks();
  }
}
