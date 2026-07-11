package caliniya.armavoke.io;

import arc.struct.StringMap;
import arc.util.io.*;
import arc.util.*;
import caliniya.armavoke.system.*;
import java.io.*;
import caliniya.armavoke.game.data.*;

public class DataIO {
  // 以下三个数据流存储实体数据
  public static volatile ByteArrayOutputStream bos = new ByteArrayOutputStream(1 << 20); // 预分配 1MB
  public static volatile DataOutputStream stream = new DataOutputStream(bos);
  public static volatile Writes w = new Writes(stream);
  public byte[] meta;

  // 命令实体处理线程开始写入数据
  // 线程会使用三次循环来写入地图数据和实体数据
  // 写入地图元数据
  public static void copy(@Nullable StringMap tags) {
    w.b(GameIO.MAGIC.getBytes());
    w.i(GameIO.SAVE_VERSION);
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
    Systems.EP.task = true;
  }
}
