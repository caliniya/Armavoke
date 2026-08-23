package caliniya.armavoke.ecs.runtime;

import arc.files.Fi;
import arc.struct.ObjectIntMap;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.io.Writes;
import caliniya.armavoke.base.game.Entity;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.io.DataIO;
import caliniya.armavoke.io.GameIO;
import caliniya.armavoke.type.Building;
import caliniya.armavoke.world.ENVBlock;
import caliniya.armavoke.world.Floor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;

/** Save coordinator for terrain plus the authoritative generated-ECS snapshot. */
public final class EcsPersistence {
  public static final int sectionMarker = 0x45435331;
  private static final int attachmentMarker = 0x45435341;
  private static SaveRequest pendingSave;
  private static byte[] pendingRestore;
  private static boolean saving;

  private EcsPersistence() {}

  public static synchronized boolean request(Fi target, StringMap sourceTags) {
    if (target == null || saving || pendingSave != null) return false;
    StringMap tags = new StringMap();
    if (sourceTags != null) for (StringMap.Entry entry : sourceTags) {
      tags.put(String.valueOf(entry.key), String.valueOf(entry.value));
    }
    pendingSave = new SaveRequest(target, tags);
    saving = true;
    DataIO.copyed = false;
    DataIO.data = null;
    return true;
  }

  public static synchronized boolean isSaving() { return saving || pendingSave != null; }

  public static void update(EcsWorld world) {
    SaveRequest request;
    synchronized (EcsPersistence.class) {
      request = pendingSave;
      pendingSave = null;
    }
    if (request == null) return;
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream(1 << 20);
      DataOutputStream stream = new DataOutputStream(bytes);
      Writes writes = new Writes(stream);
      writeEnvelope(writes, request.tags);
      ByteArrayOutputStream ecsBytes = new ByteArrayOutputStream(32 * 1024);
      try (DataOutputStream ecsOutput = new DataOutputStream(ecsBytes)) {
        world.write(ecsOutput);
        writeAttachments(ecsOutput, world);
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
      synchronized (EcsPersistence.class) { saving = false; }
    }
  }

  private static void writeEnvelope(Writes writes, StringMap tags) {
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
    Ar<Floor> floors = new Ar<>();
    ObjectIntMap<Floor> floorIds = new ObjectIntMap<>();
    Ar<ENVBlock> blocks = new Ar<>();
    ObjectIntMap<ENVBlock> blockIds = new ObjectIntMap<>();
    floors.add((Floor) null);
    blocks.add((ENVBlock) null);
    for (int y = 0; y < WorldData.world.H; y++) for (int x = 0; x < WorldData.world.W; x++) {
      Floor floor = WorldData.world.getFloor(x, y);
      ENVBlock block = WorldData.world.getENVBlock(x, y);
      if (floor != null && !floorIds.containsKey(floor)) { floorIds.put(floor, floors.size); floors.add(floor); }
      if (block != null && !blockIds.containsKey(block)) { blockIds.put(block, blocks.size); blocks.add(block); }
    }
    writes.s((short) floors.size);
    for (Floor floor : floors) writes.str(floor == null ? "null" : floor.internalName);
    writes.s((short) blocks.size);
    for (ENVBlock block : blocks) writes.str(block == null ? "null" : block.internalName);
    for (int y = 0; y < WorldData.world.H; y++) for (int x = 0; x < WorldData.world.W; x++) {
      Floor floor = WorldData.world.getFloor(x, y);
      ENVBlock block = WorldData.world.getENVBlock(x, y);
      writes.s(floor == null ? 0 : floorIds.get(floor, 0));
      writes.s(block == null ? 0 : blockIds.get(block, 0));
    }
    // Legacy records are intentionally empty. All gameplay content follows in the ECS section.
    writes.i(0);
    writes.i(0);
  }

  private static void writeAttachments(DataOutputStream output, EcsWorld world) throws Exception {
    output.writeInt(attachmentMarker);
    int count = 0;
    for (EcsEntity value : world.snapshot()) if (value instanceof Entity && value.active()) count++;
    output.writeInt(count);
    for (EcsEntity value : world.snapshot()) {
      if (!(value instanceof Entity entity) || !value.active()) continue;
      output.writeInt(value.id());
      output.writeInt(entity.item().capacity);
      output.writeInt(entity.item().items.length);
      for (int amount : entity.item().items) output.writeInt(amount);
      output.writeFloat(entity.liquid().capacity);
      output.writeInt(entity.liquid().liquids.length);
      for (float amount : entity.liquid().liquids) output.writeFloat(amount);
      output.writeFloat(entity.power().power);
      output.writeFloat(entity.power().powerMax);
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
      for (EcsEntity value : world.snapshot()) EcsEntityFactory.hydrate(value);
      if (input.available() >= 8 && input.readInt() == attachmentMarker) readAttachments(input, world);
      if (WorldData.world != null) {
        for (EcsEntity value : world.snapshot()) if (value instanceof Building building) {
          WorldData.world.setBuilding(building);
        }
      }
    } catch (Throwable error) {
      world.clear();
      Log.err("ECS snapshot restore failed", error);
    }
  }

  private static void readAttachments(DataInputStream input, EcsWorld world) throws Exception {
    int count = input.readInt();
    for (int i = 0; i < count; i++) {
      EcsEntity value = world.find(input.readInt());
      int itemCapacity = input.readInt();
      int itemLength = input.readInt();
      int[] items = new int[itemLength];
      for (int j = 0; j < itemLength; j++) items[j] = input.readInt();
      float liquidCapacity = input.readFloat();
      int liquidLength = input.readInt();
      float[] liquids = new float[liquidLength];
      for (int j = 0; j < liquidLength; j++) liquids[j] = input.readFloat();
      float power = input.readFloat();
      float powerMax = input.readFloat();
      if (!(value instanceof Entity entity)) continue;
      entity.item().capacity = itemCapacity;
      entity.item().items = items;
      entity.liquid().capacity = liquidCapacity;
      entity.liquid().liquids = liquids;
      entity.power().power = power;
      entity.power().powerMax = powerMax;
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
