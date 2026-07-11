package caliniya.armavoke.io;

import arc.Core;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GameIO {

  private static final String MAGIC = "AEVS";
  private static final int SAVE_VERSION = 1;

  /** 单位/建筑结束标记：8 字节 0xAE，读取未知类型时跳过到此标记 */
  private static final byte[] END_MARKER = {
    (byte) 0xAE, (byte) 0xAE, (byte) 0xAE, (byte) 0xAE,
    (byte) 0xAE, (byte) 0xAE, (byte) 0xAE, (byte) 0xAE
  };

  // ==================== IO 后台线程 ====================
  //
  // 一个常驻守护线程 + 阻塞队列。所有磁盘读写任务都排进队列，由这个线程串行执行：
  //   - 单线程 → 天然按调用顺序执行，两次保存不会互相踩踏；
  //   - 守护线程 → 不会阻止 JVM/App 退出；
  //   - 懒启动 → 第一次真正用到时才起线程；
  //   - 不用 ThreadPoolExecutor，避免 ThreadFactory 语义坑。

  private static final BlockingQueue<Runnable> ioQueue = new LinkedBlockingQueue<>();
  private static volatile Thread ioThread;

  /** 确保后台 IO 线程已启动（线程安全，懒加载）。 */
  private static void ensureIoThread() {
    if (ioThread != null) return;
    synchronized (GameIO.class) {
      if (ioThread != null) return;
      Thread t =
          new Thread(
              () -> {
                while (true) {
                  Runnable task;
                  try {
                    task = ioQueue.take(); // 队列空时阻塞等待
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break; // 被中断则退出线程
                  }
                  try {
                    task.run();
                  } catch (Throwable err) {
                    Log.err("IO task failed", err);
                  }
                }
              },
              "Armavoke-IO");
      t.setDaemon(true);
      t.start();
      ioThread = t;
    }
  }

  /** 把一个任务丢进后台 IO 线程执行。 */
  private static void submitIo(Runnable task) {
    ensureIoThread();
    ioQueue.add(task);
  }

  /** 进度回调（判空 + 裁剪到 [0,1]）。 */
  private static void report(arc.func.Floatc cb, float v) {
    if (cb != null) cb.get(v < 0f ? 0f : (v > 1f ? 1f : v));
  }

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

  // ==================== 保存 ====================

  /**
   * 异步保存（推荐）。 在【调用线程】先把游戏状态序列化成内存字节快照（一致性副本，很快），
   * 然后把字节丢到后台 IO 线程写盘，主线程立即返回，不会因磁盘 I/O 卡住。
   *
   * @param file 目标文件
   * @param tags 附加标签（可空）
   */
  public static void save(Fi file, @Nullable StringMap tags) {
    save(file, tags, null, null);
  }

  /**
   * 异步保存，带完成回调。
   *
   * @param file 目标文件
   * @param tags 附加标签（可空）
   * @param onComplete 写盘完成后的回调（在【主线程】执行，参数为是否成功；可空）
   */
  public static void save(Fi file, @Nullable StringMap tags, @Nullable arc.func.Boolc onComplete) {
    save(file, tags, onComplete, null);
  }

  /**
   * 异步保存，带完成回调 + 进度回调。
   *
   * <p><b>进度回调线程说明：</b>{@code onProgress} 在【执行序列化的线程】被调用。
   * 由于 snapshot 默认跑在调用线程（通常主线程），对大地图它会阻塞渲染，
   * 进度条在序列化阶段不会刷新，只有写盘阶段能看到 0.95→1.0。
   * 想让进度条在整段过程都流畅，需把 snapshot 放后台线程执行
   * （并保证此期间世界数据不被并发修改，例如先暂停逻辑线程）。
   *
   * @param file 目标文件
   * @param tags 附加标签（可空）
   * @param onComplete 写盘完成后回调（主线程执行，参数为是否成功；可空）
   * @param onProgress 进度回调 [0,1]（在序列化线程执行；可空）
   */
  public static void save(
      Fi file,
      @Nullable StringMap tags,
      @Nullable arc.func.Boolc onComplete,
      @Nullable arc.func.Floatc onProgress) {
    final byte[] snapshot;
    try {
      // ① 序列化到内存（大头耗时，进度 0.00 → 0.95 在此产生）
      snapshot = snapshot(tags, onProgress);
    } catch (Throwable e) {
      Log.err("Snapshot failed", e);
      if (onComplete != null) onComplete.get(false);
      return;
    }

    // ② 后台线程写盘（进度 0.95 → 1.00）
    submitIo(
        () -> {
          boolean ok = writeSnapshot(file, snapshot);
          report(onProgress, 1f);
          if (onComplete != null) {
            Core.app.post(() -> onComplete.get(ok));
          }
        });
  }

  /**
   * 同步保存（会阻塞调用线程直到写盘完成，一般不用；提供给需要"存完再退出"等场景）。 依然遵循"先内存快照，再写盘"，只是不异步。
   */
  public static void saveSync(Fi file, @Nullable StringMap tags) {
    try {
      byte[] snapshot = snapshot(tags, null);
      writeSnapshot(file, snapshot);
    } catch (Throwable e) {
      Log.err("Save(sync) failed", e);
    }
  }

  /**
   * 把当前游戏状态序列化成内存字节数组（一致性快照）。 该方法必须在【游戏逻辑线程/主线程】调用，调用期间不应有其他线程修改世界数据。
   */
  private static byte[] snapshot(@Nullable StringMap tags, @Nullable arc.func.Floatc onProgress)
      throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream(1 << 20); // 预分配 1MB
    try (DataOutputStream stream = new DataOutputStream(bos)) {
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

      // --- 准备调色板 ---
      Ar<Floor> floorPalette = new Ar<>();
      ObjectIntMap<Floor> floorMap = new ObjectIntMap<>();
      Ar<ENVBlock> blockPalette = new Ar<>();
      ObjectIntMap<ENVBlock> blockMap = new ObjectIntMap<>();

      floorPalette.add((Floor) null);
      blockPalette.add((ENVBlock) null);

      int width = WorldData.world.W;
      int height = WorldData.world.H;

      // [进度] 阶段1：扫描调色板 0.00 → 0.35
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
        report(onProgress, 0.35f * (y + 1) / height);
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

      // [进度] 阶段2：写入地图数据 0.35 → 0.70
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          Floor floor = WorldData.world.getFloor(x, y);
          ENVBlock block = WorldData.world.getENVBlock(x, y);
          w.s(floor == null ? 0 : floorMap.get(floor, 0));
          w.s(block == null ? 0 : blockMap.get(block, 0));
        }
        report(onProgress, 0.35f + 0.35f * (y + 1) / height);
      }

      // --- Units --- [进度] 0.70 → 0.85
      int unitCount = WorldData.units.size;
      w.i(unitCount);
      int ui = 0;
      for (Unit u : WorldData.units) {
        w.str(u.type.internalName);
        u.write(w);
        w.b(END_MARKER); // 结束标记
        ui++;
        if ((ui & 63) == 0 && unitCount > 0) report(onProgress, 0.70f + 0.15f * ui / unitCount);
      }
      report(onProgress, 0.85f);

      // --- Buildings --- [进度] 0.85 → 0.95
      int buildingCount = WorldData.buildings.size;
      w.i(buildingCount);
      int bi = 0;
      for (Building b : WorldData.buildings) {
        w.str(b.block.internalName);
        b.write(w);
        w.b(END_MARKER); // 结束标记
        bi++;
        if ((bi & 63) == 0 && buildingCount > 0)
          report(onProgress, 0.85f + 0.10f * bi / buildingCount);
      }
      report(onProgress, 0.95f);

      stream.flush();
    }
    return bos.toByteArray();
  }

  /** 把内存快照写入磁盘（可在任意线程调用，通常在 IO 线程）。返回是否成功。 */
  private static boolean writeSnapshot(Fi file, byte[] data) {
    try {
      file.parent().mkdirs();
      file.writeBytes(data, false);
      Log.info("Saved to @ (@ bytes)", file.path(), data.length);
      return true;
    } catch (Throwable e) {
      Log.err("Save failed", e);
      return false;
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
  private static void readBody(Reads r, int width, int height, @Nullable arc.func.Floatc onProgress) {
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

    // --- 地图数据 --- [进度] 0.00 → 0.55
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
      report(onProgress, 0.55f * (y + 1) / height);
    }

    // --- Units --- [进度] 0.55 → 0.80
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
      if ((i & 63) == 0 && unitCount > 0) report(onProgress, 0.55f + 0.25f * (i + 1) / unitCount);
    }
    report(onProgress, 0.80f);

    // --- Buildings --- [进度] 0.80 → 0.95
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
      if ((i & 63) == 0 && buildingCount > 0)
        report(onProgress, 0.80f + 0.15f * (i + 1) / buildingCount);
    }
    report(onProgress, 0.95f);

    // --- 初始化寻路 ---
    RouteData.init();
    report(onProgress, 1f);
  }

  /**
   * 从内存字节数据解析并重建世界（直接读完整 header）。 必须在【主线程】调用，因为会创建游戏对象、修改 WorldData。
   */
  private static void applyFromBytes(byte[] data, @Nullable arc.func.Floatc onProgress)
      throws IOException {
    try (DataInputStream stream = new DataInputStream(new ByteArrayInputStream(data))) {
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
      readBody(r, width, height, onProgress);
    }
  }

  // ==================== 加载 ====================

  /**
   * 异步加载（推荐）。 后台线程把文件读成字节（慢的磁盘 I/O 不在主线程），读完再切回【主线程】解析、重建世界，
   * 保证造 Unit/Building、改 WorldData 都在主线程执行。
   *
   * @param file 存档文件
   * @param onComplete 完成回调（主线程执行，参数为是否成功；可空）
   */
  public static void loadAsync(Fi file, @Nullable arc.func.Boolc onComplete) {
    loadAsync(file, onComplete, null);
  }

  /**
   * 异步加载，带进度回调。
   *
   * <p><b>线程说明：</b>读盘在后台线程；解析/重建世界（applyFromBytes）切回【主线程】执行，
   * 所以 {@code onProgress} 在主线程被调用。对大地图，解析会阻塞主线程/渲染，
   * 进度条同样可能不流畅——建议加载时用加载遮罩挡住画面，或把解析也放后台
   * （需保证渲染线程此刻不遍历 WorldData）。
   *
   * @param file 存档文件
   * @param onComplete 完成回调（主线程执行，参数为是否成功；可空）
   * @param onProgress 进度回调 [0,1]（主线程执行；可空）
   */
  public static void loadAsync(
      Fi file, @Nullable arc.func.Boolc onComplete, @Nullable arc.func.Floatc onProgress) {
    submitIo(
        () -> {
          final byte[] data;
          try {
            data = file.readBytes(); // 后台读盘
          } catch (Throwable e) {
            Log.err("Load(async) read failed", e);
            if (onComplete != null) Core.app.post(() -> onComplete.get(false));
            return;
          }
          // 切回主线程解析 + 应用
          Core.app.post(
              () -> {
                boolean ok = true;
                try {
                  applyFromBytes(data, onProgress);
                } catch (Throwable e) {
                  Log.err("Load(async) parse failed", e);
                  WorldData.initWorld();
                  ok = false;
                }
                if (onComplete != null) onComplete.get(ok);
              });
        });
  }

  /** 异步加载（无回调重载）。 */
  public static void loadAsync(Fi file) {
    loadAsync(file, null);
  }

  /** 异步加载（从 Map 元数据）。 */
  public static void loadAsync(Map map, @Nullable arc.func.Boolc onComplete) {
    loadAsync(map.file, onComplete);
  }

  // -------------------- 同步加载（保留，兼容旧调用） --------------------

  /** 从 Map 元数据对象加载存档（同步，会阻塞调用线程）。 */
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
      readBody(r, map.width, map.height, null);

    } catch (IOException e) {
      Log.err("Load(Map) failed", e);
      WorldData.initWorld();
    }
  }

  /** 从文件直接加载存档（同步，会阻塞调用线程）。 */
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
      readBody(r, width, height, null);

    } catch (IOException e) {
      Log.err("Load failed", e);
      WorldData.initWorld();
    }
  }
}
