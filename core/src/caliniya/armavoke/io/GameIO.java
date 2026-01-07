package caliniya.armavoke.io;

import arc.files.Fi;
import arc.struct.ObjectIntMap;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.core.ContentVar;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.RouteData;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.game.type.UnitType;
import caliniya.armavoke.map.Map;
import caliniya.armavoke.world.ENVBlock;
import caliniya.armavoke.world.Floor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GameIO {

  private static final String MAGIC = "AEVS";
  private static final int SAVE_VERSION = 1;

  public static Map readMeta(Fi file) {
    try (DataInputStream stream = new DataInputStream(file.read())) {
      Reads r = new Reads(stream);
      String magic = new String(r.b(4));
      if (!magic.equals(MAGIC)) return null;
      int ver = r.i();
      int w = r.i();
      int h = r.i();
      StringMap tags = new StringMap();
      int tagCount = r.s();
      for (int i = 0; i < tagCount; i++) tags.put(r.str(), r.str());
      return new Map(file, w, h, tags, true);
    } catch (IOException e) {
      return null;
    }
  }

  public static void save(Fi file, @Nullable StringMap tags) {
    try (DataOutputStream stream = new DataOutputStream(file.write(false))) {
      Writes w = new Writes(stream);

      // --- Header ---
      w.b(MAGIC.getBytes());
      w.i(SAVE_VERSION);
      w.i(WorldData.world.W);
      w.i(WorldData.world.H);

      // --- Tags ---
      if (tags == null) tags = new StringMap();
      tags.put("space", String.valueOf(WorldData.world.space));
      w.s(tags.size);
      for (var entry : tags) {
        w.str(entry.key);
        w.str(entry.value);
      }

      // --- 准备调色板 (Palette) ---
      Ar<Floor> floorPalette = new Ar<>();
      ObjectIntMap<Floor> floorMap = new ObjectIntMap<>();
      Ar<ENVBlock> blockPalette = new Ar<>();
      ObjectIntMap<ENVBlock> blockMap = new ObjectIntMap<>();

      floorPalette.add((Floor) null);
      blockPalette.add((ENVBlock) null);
      // 注意：ObjectIntMap 默认值是 0，正好对应 null

      int width = WorldData.world.W;
      int height = WorldData.world.H;

      // 第一遍扫描：统计用到了哪些方块
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          // 使用 API 获取，而不是访问数组
          Floor floor = WorldData.world.getFloor(x, y);
          ENVBlock block = WorldData.world.getENVBlock(x, y);

          if (floor != null && !floorMap.containsKey(floor)) {
            floorMap.put(floor, floorPalette.size);
            floorPalette.add(floor);
          }

          if (block != null && !blockMap.containsKey(block)) {
            blockMap.put(block, blockPalette.size);
            blockPalette.add(block);
          }
        }
      }

      // 写入调色板
      w.s(floorPalette.size);
      for (int i = 0; i < floorPalette.size; i++) {
        Floor f = floorPalette.get(i);
        w.str(f == null ? "null" : f.internalName);
      }

      w.s(blockPalette.size);
      for (int i = 0; i < blockPalette.size; i++) {
        ENVBlock b = blockPalette.get(i);
        w.str(b == null ? "null" : b.internalName);
      }

      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          Floor floor = WorldData.world.getFloor(x, y);
          ENVBlock block = WorldData.world.getENVBlock(x, y);

          w.s(floor == null ? 0 : floorMap.get(floor, 0));
          w.s(block == null ? 0 : blockMap.get(block, 0));
        }
      }

      // --- Units ---
      w.i(WorldData.units.size);
      for (Unit u : WorldData.units) {
        w.str(u.type.internalName);
        u.write(w);
      }

      Log.info("Saved to @", file.path());

    } catch (IOException e) {
      Log.err("Save failed", e);
    }
  }

  public static void load(Map map) {
    Log.info("Loading map: @", map.name());
    load(map.file);
  }

  public static void load(Fi file) {
    try (DataInputStream stream = new DataInputStream(file.read())) {
      Reads r = new Reads(stream);

      // Magic
      String magic = new String(r.b(4));
      if (!magic.equals(MAGIC)) throw new IOException("Invalid file format");

      int ver = r.i();
      int width = r.i();
      int height = r.i();

      // --- Tags 读取逻辑修改 ---
      StringMap tags = new StringMap();
      int tagCount = r.s();
      for (int i = 0; i < tagCount; i++) {
        String key = r.str();
        String value = r.str();
        tags.put(key, value);
      }

      // 获取是否为太空地图 (默认为 false)
      boolean isSpace = tags.getBool("space");

      WorldData.reBuildAll(width, height, isSpace);

      // 读取调色板
      int floorPaletteSize = r.s();
      Floor[] floorLookup = new Floor[floorPaletteSize];
      for (int i = 0; i < floorPaletteSize; i++) {
        String name = r.str();
        floorLookup[i] = name.equals("null") ? null : ContentVar.get(name, Floor.class);
      }

      int blockPaletteSize = r.s();
      ENVBlock[] blockLookup = new ENVBlock[blockPaletteSize];
      for (int i = 0; i < blockPaletteSize; i++) {
        String name = r.str();
        blockLookup[i] = name.equals("null") ? null : ContentVar.get(name, ENVBlock.class);
      }
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          short floorId = r.s();
          short blockId = r.s();

          Floor floor =
              (floorId >= 0 && floorId < floorLookup.length) ? floorLookup[floorId] : null;
          ENVBlock block =
              (blockId >= 0 && blockId < blockLookup.length) ? blockLookup[blockId] : null;

          // 使用 API 设置数据
          WorldData.world.setFloor(x, y, floor);
          WorldData.world.setENVBlock(x, y, block);
        }
      }

      // Units
      int unitCount = r.i();
      for (int i = 0; i < unitCount; i++) {
        String typeName = r.str();
        UnitType type = ContentVar.get(typeName, UnitType.class);
        if (type != null) {
          Unit u = Unit.create(type);
          u.read(r);
        }
      }

      // 初始化寻路
      RouteData.init();

      // 重建渲染
      // caliniya.armavoke.Armavoke.mapRender.rebuildAll();

    } catch (IOException e) {
      Log.err("Load failed", e);
      WorldData.initWorld();
    }
  }
}
