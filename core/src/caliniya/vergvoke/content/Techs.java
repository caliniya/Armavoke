package caliniya.vergvoke.content;

import caliniya.vergvoke.campaign.TechNode;
import caliniya.vergvoke.campaign.TechTree;

/**
 * 科技树定义（当前为测试用，使用现有内容）。
 *
 * <p>结构：
 *
 * <pre>
 * base（唯一根）
 * ├── turret   （研究后解锁 Blocks.testTurret）
 * │   ├── building（Blocks.TestBlock，需要先研究 turret）
 * │   └── unit     （UnitTypes.test，需要先研究 turret）
 * </pre>
 */
public class Techs {

  public static TechTree tree;

  public static void load() {
    tree = new TechTree();

    TechNode base = tree.root("base");

    TechNode turret = tree.addChild(base, Blocks.testTurret);
    TechNode building = tree.addChild(turret, Blocks.TestBlock);
    TechNode unit = tree.addChild(turret, UnitTypes.test);

    tree.updateUnlocks();
  }
}
