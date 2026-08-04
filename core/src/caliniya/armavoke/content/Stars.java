package caliniya.armavoke.content;

import caliniya.armavoke.world.stars.StarMap;
import caliniya.armavoke.world.stars.StarNode;

/**
 * 星域与星系节点的定义（代码声明，和 Blocks/UnitTypes 一样）。
 *
 * <p>约定：节点原始名称 == 它对应地图的内部名（campaign/星域/maps/节点名.aes）。
 *
 * <p>星域/节点关系由代码定义，运行时不需要序列化它们；
 * 存档只写进度文件（ProgressData）+ 玩过的地图副本。
 */
public class Stars {

  public static StarMap demo;

  public static void load() {
    demo = new StarMap("demo", 2000, 2000);

    StarNode start = new StarNode(200, 200, "start");
    StarNode mid = new StarNode(1000, 1000, "mid");
    StarNode boss = new StarNode(1800, 1800, "boss");
    boss.size = 96;

    demo.addNode(start, mid, boss);
    demo.link(start, mid);
    demo.link(mid, boss);
  }
}
