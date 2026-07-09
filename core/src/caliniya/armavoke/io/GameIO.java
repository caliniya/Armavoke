package caliniya.armavoke.io;

import arc.files.Fi;
import arc.struct.ObjectIntMap;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.game.Building;
import caliniya.armavoke.game.Contents;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.RouteData;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.game.type.UnitType;
import caliniya.armavoke.map.Map;
import caliniya.armavoke.world.Block;
import caliniya.armavoke.world.ENVBlock;
import caliniya.armavoke.world.Floor;
import caliniya.armavoke.base.game.WorldChunk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GameIO {

  private static final String MAGIC = "AEVS";
  private static final int SAVE_VERSION = 1;

  /** 单位/建筑结束标记：8 字节 0xAE，读取未知类型时跳过到此标记 */
  private static final byte[] END_MARKER = {
    (byte) 0xAE, (byte) 0xAE, (byte) 0xAE, (byte) 0xAE,
    (byte) 0xAE, (byte) 0xAE, (byte) 0xAE, (byte) 0xAE
  };

  // ==================== 元数据读取 ====================

  /** 快速读取存档头，返回 Map 元数据对象（不加载游戏数据） */
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

  // ==================== 存档 ====================

  public static void save(Fi file, @Nullable StringMap tags) {
    file.parent().mkdirs();
    try (DataOutputStream stream = new DataOutputStream(file.write(false))) {
      Writes w = new Writes(stream);

      // file.mkdirs();
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

      // --- 准备调色板 ---
      Ar<Floor> floorPalette = new Ar<>();
      ObjectIntMap<Floor> floorMap = new ObjectIntMap<>();
      Ar<ENVBlock> blockPalette = new Ar<>();
      ObjectIntMap<ENVBlock> blockMap = new ObjectIntMap<>();

      floorPalette.add((Floor) null);
      blockPalette.add((ENVBlock) null);

      int width = WorldData.world.W;
      int height = WorldData.world.H;

      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
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

      // 写入地图数据
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
        w.b(END_MARKER); // 结束标记
      }

      // --- Buildings ---
      w.i(WorldData.buildings.size);
      for (Building b : WorldData.buildings) {
        w.str(b.block.internalName);
        b.write(w);
        w.b(END_MARKER); // 结束标记
      }

      Log.info("Saved to @", file.path());

    } catch (IOException e) {
      Log.err("Save failed", e);
    }
  }

  // ==================== 读取辅助 ====================

  /**
   * 从当前读取位置开始，一路跳过字节直到找到 END_MARKER（8 个 0xAE）。 调用前假设 Reader 正处于未知数据的开头，调用后 Reader 位于 END_MARKER 之后。
   */
  private static void skipToEndMarker(Reads r) {
    int matched = 0;
    while (matched < 8) {
      byte b = (byte) r.b();
      if (b == END_MARKER[matched]) {
        matched++;
      } else {
        matched = 0;
        if (b == END_MARKER[0]) matched = 1;
      }
    }
  }

  /** 读取存档 body：调色板 → 地图数据 → 单位 → 建筑 → 寻路初始化。 调用前 WorldData.world 必须已经通过 reBuildAll 初始化。 */
  private static void readBody(Reads r, int width, int height) {
    // --- 调色板 ---
    int floorPaletteSize = r.s();
    Floor[] floorLookup = new Floor[floorPaletteSize];
    for (int i = 0; i < floorPaletteSize; i++) {
      String name = r.str();
      floorLookup[i] = name.equals("null") ? null : Contents.get(name, Floor.class);
    }

    int blockPaletteSize = r.s();
    ENVBlock[] blockLookup = new ENVBlock[blockPaletteSize];
    for (int i = 0; i < blockPaletteSize; i++) {
      String name = r.str();
      blockLookup[i] = name.equals("null") ? null : Contents.get(name, ENVBlock.class);
    }

    // --- 地图数据 ---
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        short floorId = r.s();
        short blockId = r.s();
        Floor floor = (floorId >= 0 && floorId < floorLookup.length) ? floorLookup[floorId] : null;
        ENVBlock block =
            (blockId >= 0 && blockId < blockLookup.length) ? blockLookup[blockId] : null;
        WorldData.world.setFloor(x, y, floor);
        WorldData.world.setENVBlock(x, y, block);
      }
    }

    // --- Units ---
    int unitCount = r.i();
    for (int i = 0; i < unitCount; i++) {
      String typeName = r.str();
      UnitType type = Contents.get(typeName, UnitType.class);
      if (type != null) {
        Unit u = type.create();
        u.read(r);
        skipToEndMarker(r); // 校验结束标记
      } else {
        Log.warn("Unknown unit type in save: @, skipping...", typeName);
        skipToEndMarker(r);
      }
    }

    // --- Buildings ---
    int buildingCount = r.i();
    for (int i = 0; i < buildingCount; i++) {
      String typeName = r.str();
      Block type = Contents.get(typeName, Block.class);
      if (type != null) {
        Building b = type.create(0, 0);
        b.read(r);
        skipToEndMarker(r); // 校验结束标记
        WorldData.buildings.add(b);
        b.getOccupiedCoords((tx, ty) -> WorldData.world.setBuilding(tx, ty, b.block));
      } else {
        Log.warn("Unknown block type in save: @, skipping...", typeName);
        skipToEndMarker(r);
      }
    }

    // --- 初始化寻路 ---
    RouteData.init();
  }

  // ==================== 加载 ====================

  /** 从 Map 元数据对象加载存档（Map 由 readMeta 预先读取头信息） */
  public static void load(Map map) {
    try (DataInputStream stream = new DataInputStream(map.file.read())) {
      Reads r = new Reads(stream);

      // 跳过 readMeta 已读的 header
      r.b(4); // MAGIC
      r.i(); // version
      r.i(); // width (skip, use map.width)
      r.i(); // height (skip, use map.height)

      // 跳过 tags
      int tagCount = r.s();
      for (int i = 0; i < tagCount; i++) {
        r.str();
        r.str();
      }

      WorldData.reBuildAll(map.width, map.height, map.space);
      readBody(r, map.width, map.height);

    } catch (IOException e) {
      Log.err("Load(Map) failed", e);
      WorldData.initWorld();
    }
  }

  /** 从文件直接加载存档 */
  public static void load(Fi file) {
    try (DataInputStream stream = new DataInputStream(file.read())) {
      Reads r = new Reads(stream);

      String magic = new String(r.b(4));
      if (!magic.equals(MAGIC)) throw new IOException("Invalid file format");

      int ver = r.i();
      int width = r.i();
      int height = r.i();

      StringMap tags = new StringMap();
      int tagCount = r.s();
      for (int i = 0; i < tagCount; i++) {
        tags.put(r.str(), r.str());
      }
      boolean isSpace = tags.getBool("space");

      WorldData.reBuildAll(width, height, isSpace);
      readBody(r, width, height);

    } catch (IOException e) {
      Log.err("Load failed", e);
      WorldData.initWorld();
    }
  }
}
