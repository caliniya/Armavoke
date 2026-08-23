package caliniya.armavoke.ecs.runtime;

import arc.files.Fi;
import arc.struct.ObjectIntMap;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.io.Writes;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.io.DataIO;
import caliniya.armavoke.io.GameIO;
import caliniya.armavoke.type.Building;
import caliniya.armavoke.type.Unit;
import caliniya.armavoke.world.ENVBlock;
import caliniya.armavoke.world.Floor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;

/** Backward-compatible save coordinator with an appended generated-ECS snapshot. */
public final class EcsPersistence {
  public static final int sectionMarker = 0x45435331;
  private static SaveRequest pendingSave;
  private static byte[] pendingRestore;
  private static boolean saving;

  private EcsPersistence() {}

  public static synchronized boolean request(Fi target, StringMap sourceTags) {
    if (target == null || saving || pendingSave != null) return false;
    StringMap tags = new StringMap();
    if (sourceTags != null) {
      for (StringMap.Entry entry : sourceTags) {
        tags.put(String.valueOf(entry.key), String.valueOf(entry.value));
      }
    }
    pendingSave = new SaveRequest(target, tags);
    saving = true;
    DataIO.copyed = false;
    DataIO.data = null;
    return true;
  }

  public static synchronized boolean isSaving() {
    return saving || pendingSave != null;
  }

  public static void update(EcsWorld world) {
    SaveRequest request;
    synchronized (EcsPersistence.class) {
      request = pendingSave;
      pendingSave = null;
    }
    if (request == null) return;
    try {
      GameEcsBridge.syncAllFromLegacy();
      ByteArrayOutputStream bytes = new ByteArrayOutputStream(1 << 20);
      DataOutputStream stream = new DataOutputStream(bytes);
      Writes writes = new Writes(stream);
      writeLegacy(writes, request.tags);
      ByteArrayOutputStream ecsBytes = new ByteArrayOutputStream(16 * 1024);
      try (DataOutputStream ecsOutput = new DataOutputStream(ecsBytes)) {
        world.write(ecsOutput);
      }
      byte[] section = ecsBytes.toByteArray();
      writes.i(sectionMarker);
      writes.i(section.length);
      writes.b(section);
      stream.flush();
      DataIO.bos = bytes;
      DataIO.stream = stream;
      DataIO.w = writes;
      DataIO.data = bytes.toByteArray();
      DataIO.saveTarget = request.target;
      DataIO.copyed = true;
      GameIO.save(request.target);
    } catch (Throwable error) {
      Log.err("ECS save failed", error);
    } finally {
      synchronized (EcsPersistence.class) {
        saving = false;
      }
    }
  }

  private static void writeLegacy(Writes writes, StringMap tags) {
    writes.b(GameIO.MAGIC.getBytes(StandardCharsets.US_ASCII));
    writes.i(GameIO.SAVE_VERSION);
    writes.i(WorldData.world.W);
    writes.i(WorldData.world.H);
    tags.put("space", String.valueOf(WorldData.world.space));
    writes.s(tags.size);
    for (StringMap.Entry entry : tags) {
      writes.str(String.valueOf(entry.key));
      writes.str(String.valueOf(entry.value));
    }

    Ar<Floor> floorPalette = new Ar<>();
    ObjectIntMap<Floor> floorMap = new ObjectIntMap<>();
    Ar<ENVBlock> blockPalette = new Ar<>();
    ObjectIntMap<ENVBlock> blockMap = new ObjectIntMap<>();
    floorPalette.add((Floor) null);
    blockPalette.add((ENVBlock) null);
    for (int y = 0; y < WorldData.world.H; y++) {
      for (int x = 0; x < WorldData.world.W; x++) {
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
    writes.s((short) floorPalette.size);
    for (Floor floor : floorPalette) writes.str(floor == null ? "null" : floor.internalName);
    writes.s((short) blockPalette.size);
    for (ENVBlock block : blockPalette) writes.str(block == null ? "null" : block.internalName);
    for (int y = 0; y < WorldData.world.H; y++) {
      for (int x = 0; x < WorldData.world.W; x++) {
        Floor floor = WorldData.world.getFloor(x, y);
        ENVBlock block = WorldData.world.getENVBlock(x, y);
        writes.s(floor == null ? 0 : floorMap.get(floor, 0));
        writes.s(block == null ? 0 : blockMap.get(block, 0));
      }
    }

    Ar<Unit> units = new Ar<>();
    WorldData.units.each(unit -> {
      if (unit != null && unit.health > 0f) units.add(unit);
    });
    writes.i(units.size);
    for (Unit unit : units) {
      writes.str(unit.type.internalName);
      unit.write(writes);
      writes.b(DataIO.END_MARKER);
    }
    Ar<Building> buildings = new Ar<>();
    WorldData.buildings.each(building -> {
      if (building != null && building.health > 0f) buildings.add(building);
    });
    writes.i(buildings.size);
    for (Building building : buildings) {
      writes.str(building.block.internalName);
      building.write(writes);
      writes.b(DataIO.END_MARKER);
    }
  }

  public static synchronized void queueRestore(byte[] bytes) {
    pendingRestore = bytes == null ? null : bytes.clone();
  }

  public static void restorePending(EcsWorld world) {
    byte[] bytes;
    synchronized (EcsPersistence.class) {
      bytes = pendingRestore;
      pendingRestore = null;
    }
    if (bytes == null || bytes.length == 0) return;
    try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
      world.read(input);
    } catch (Throwable error) {
      world.clear();
      Log.err("ECS snapshot restore failed; legacy save data remains available", error);
    }
  }

  private static final class SaveRequest {
    final Fi target;
    final StringMap tags;

    SaveRequest(Fi target, StringMap tags) {
      this.target = target;
      this.tags = tags;
    }
  }
}
