package caliniya.armavoke.io;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import caliniya.armavoke.map.Map;
import caliniya.armavoke.world.stars.StarMap;

import java.io.*;

/**
 * 星域（StarMap）存档 IO。一个星域 = 一个存档文件。
 *
 * <p>文件格式（与 DataIO 风格一致，tags 放在节点数据之前以便快速读元数据）：
 *
 * <pre>
 *   MAGIC("AESS") + VERSION(int) + w/h(float)
 *   tags(short count + [str key, str value]×n)
 *   starCount(int) + [节点: id, x, y, size, name, 邻居数, 邻居id[]]×starCount
 * </pre>
 *
 * 道路不直接存储，读档时由邻接表通过 {@link StarMap#link} 重建。
 */
public class WorldIO {

  /** 星域存档魔法数（与地图存档 "AEVS" 区分开） */
  public static final String MAGIC = "AESS";

  public static final int SAVE_VERSION = 1;

  private static final ByteArrayOutputStream bos = new ByteArrayOutputStream(1 << 16);
  private static final DataOutputStream stream = new DataOutputStream(bos);
  private static final Writes w = new Writes(stream);

  private WorldIO() {}

  /** 序列化星域到字节数组（同步，数据量小） */
  public static synchronized byte[] write(StarMap map, StringMap tags) {
    try {
      bos.reset();
      w.b(MAGIC.getBytes());
      w.i(SAVE_VERSION);
      map.write(w);

      if (tags == null) tags = new StringMap();
      w.s(tags.size);
      for (var entry : tags) {
        w.str(entry.key);
        w.str(entry.value);
      }
      stream.flush();
      return bos.toByteArray();
    } catch (Throwable e) {
      Log.err("StarMap serialize failed", e);
      return null;
    }
  }

  public static void save(Fi file, StarMap map) {
    save(file, map, null);
  }

  /** 保存星域，落盘走 GameIO 的后台 IO 线程 */
  public static void save(Fi file, StarMap map, StringMap tags) {
    byte[] bytes = write(map, tags);
    if (bytes == null) return;
    GameIO.submitIo(
        () -> {
          try {
            file.writeBytes(bytes);
            Log.info("StarMap save success - @", file.absolutePath());
          } catch (Throwable e) {
            Log.err("StarMap save failed -", e);
          }
        });
  }

  /** 从文件读档（后台 IO），完成后在主线程回调 */
  public static void load(Fi file, Cons<StarMap> onLoad) {
    GameIO.submitIo(
        () -> {
          try {
            byte[] bytes = file.readBytes();
            StarMap map = read(bytes);
            Core.app.post(
                () -> {
                  if (onLoad != null) onLoad.get(map);
                });
          } catch (Throwable e) {
            Log.err("StarMap load failed", e);
            Core.app.post(
                () -> {
                  if (onLoad != null) onLoad.get(null);
                });
          }
        });
  }

  /** 从字节流反序列化星域 */
  public static StarMap read(byte[] bytes) {
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
      Reads r = new Reads(in);
      String magic = new String(r.b(4));
      if (!magic.equals(MAGIC)) throw new IOException("Invalid star map file");
      int ver = r.i();
      if (ver > SAVE_VERSION) throw new IOException("Unsupported star map version: " + ver);
      return StarMap.read(r);
    } catch (Throwable e) {
      Log.err("StarMap deserialize failed", e);
      return null;
    }
  }

  /** 快速读取星域存档头（尺寸 + tags），返回 Map 元数据供列表展示 */
  public static Map readMeta(Fi file) {
    try (DataInputStream stream = new DataInputStream(file.read())) {
      Reads r = new Reads(stream);
      String magic = new String(r.b(4));
      if (!magic.equals(MAGIC)) return null;
      int ver = r.i();
      if (ver > SAVE_VERSION) return null;
      float w = r.f();
      float h = r.f();
      StringMap tags = new StringMap();
      int tagCount = r.s();
      for (int i = 0; i < tagCount; i++) {
        tags.put(r.str(), r.str());
      }
      return new Map(file, (int) w, (int) h, tags, true);
    } catch (IOException e) {
      return null;
    }
  }
}
