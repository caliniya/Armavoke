package caliniya.armavoke.ui.windows;

import arc.Core;
import arc.files.Fi;
import arc.scene.ui.layout.Table;
import arc.struct.StringMap;
import arc.util.Log;
import caliniya.armavoke.campaign.Campaign;
import caliniya.armavoke.core.Data;
import caliniya.armavoke.game.Game;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.io.DataIO;
import caliniya.armavoke.io.WorldIO;
import caliniya.armavoke.system.Systems;
import caliniya.armavoke.ui.Button;

/** 集中验证进度、地图、星域与内置地图回退路径。 */
public class IoTestWindow extends Window {

  private static final String testDir = "io-test";

  public IoTestWindow() {
    super("IO 测试");
    showFullButton = false;
  }

  @Override
  public void main(Table table) {
    table.defaults().width(220f).height(52f).pad(4f);
    add(table, "保存进度", this::saveProgress);
    add(table, "读取进度", this::loadProgress);
    add(table, "保存当前地图", this::saveMap);
    add(table, "读取测试地图", this::loadMap);
    add(table, "保存当前星域", this::saveStarMap);
    add(table, "读取测试星域", this::loadStarMap);
    add(table, "内置地图回退", this::loadFallbackMap);
  }

  private void add(Table table, String text, Runnable action) {
    table.add(new Button(text, action));
    table.row();
  }

  private Fi testFile(String name) {
    Fi dir = Core.settings.getDataDirectory().child(testDir);
    if (!dir.exists()) dir.mkdirs();
    return dir.child(name);
  }

  private void saveProgress() {
    Campaign.saveProgress();
    Log.info("[IO Test] progress saved -> @", Campaign.progress().maps.size);
  }

  private void loadProgress() {
    Campaign.loadProgress();
    Log.info("[IO Test] progress loaded -> @", Campaign.progress().maps.size);
  }

  private void saveMap() {
    if (WorldData.world == null || Systems.ECS == null || !Systems.ECS.inited) {
      Log.warn("[IO Test] no active world to save");
      return;
    }
    StringMap tags = new StringMap();
    tags.put("name", "io-test-map");
    tags.put("author", "Armavoke IO Test");
    DataIO.setSave(testFile("map-test.aevs"), tags);
    Log.info("[IO Test] map save queued");
  }

  private void loadMap() {
    Fi file = testFile("map-test.aevs");
    if (!file.exists()) {
      Log.warn("[IO Test] map-test.aevs does not exist; save it first");
      return;
    }
    Data.load(file, () -> Log.info("[IO Test] map loaded -> @", file.absolutePath()));
  }

  private void saveStarMap() {
    if (Game.starMap == null) {
      Log.warn("[IO Test] no active star map to save");
      return;
    }
    StringMap tags = new StringMap();
    tags.put("name", "io-test-star");
    WorldIO.save(testFile("star-test.aess"), Game.starMap, tags);
    Log.info("[IO Test] star map save queued");
  }

  private void loadStarMap() {
    Fi file = testFile("star-test.aess");
    if (!file.exists()) {
      Log.warn("[IO Test] star-test.aess does not exist; save it first");
      return;
    }
    WorldIO.load(
        file,
        map -> {
          if (map == null) {
            Log.warn("[IO Test] star map load failed");
          } else {
            Game.starMap = map;
            Log.info("[IO Test] star map loaded -> @ nodes", map.nodeSet.size);
          }
        });
  }

  private void loadFallbackMap() {
    String relative = "campaign/test/map/a1.aevs";
    Fi local = Core.settings.getDataDirectory().child(relative);
    Fi builtin = Core.files.internal(relative);
    Fi selected = local.exists() ? local : builtin;
    if (!selected.exists()) {
      Log.warn("[IO Test] neither local nor built-in fallback map exists");
      return;
    }
    Log.info("[IO Test] fallback selected @ map -> @", local.exists() ? "local" : "built-in", selected);
    Data.load(selected, () -> Log.info("[IO Test] fallback map loaded"));
  }
}
