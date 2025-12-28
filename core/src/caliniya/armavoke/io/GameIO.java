package caliniya.armavoke.io;

import arc.files.Fi;
import arc.struct.ObjectIntMap;
import arc.struct.Seq;
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
import caliniya.armavoke.world.Block;
import caliniya.armavoke.world.ENVBlock;
import caliniya.armavoke.world.Floor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GameIO {
  //各种进制表示()
  //41 52 4D 41 56 4F 4B 45
  //65  82  77  65  86  79  75  69 
  //E843693BC3831D6954E55EF5B583195D2423E943
  //146D2A0B6CA06F940DCF7E43CB5E067B
  //6291349D
  //QVJNQVZPS0U=
  //01000001 01010010 01001101 01000001 01010110 01001111 01001011 01000101
  private static final String MAGIC = "ARMAVOKE";
  private static final int SAVE_VERSION = 1;

  public static Map readMeta(Fi file) {
    try (DataInputStream stream = new DataInputStream(file.read())) {
        Reads r = new Reads(stream);
        String magic = new String(r.b(8));
        if (!magic.equals(MAGIC)) return null;
        int ver = r.i();
        int w = r.i();
        int h = r.i();
        StringMap tags = new StringMap();
        int tagCount = r.s();
        for (int i = 0; i < tagCount; i++) tags.put(r.str(), r.str());
        return new Map(file, w, h, tags, true);
    } catch (IOException e) { return null; }
  }

  public static void save(Fi file, @Nullable StringMap tags) {
    try (DataOutputStream stream = new DataOutputStream(file.write(false))) {
      Writes w = new Writes(stream);

      // Magic & Header
      w.b(MAGIC.getBytes());
      w.i(SAVE_VERSION);
      w.i(WorldData.world.W);
      w.i(WorldData.world.H);

      // Tags
      if (tags == null) tags = new StringMap();
      w.s(tags.size);
      for (var entry : tags) {
        w.str(entry.key);
        w.str(entry.value);
      }
      
      Ar<Floor> floorPalette = new Ar<>();
      ObjectIntMap<Floor> floorMap = new ObjectIntMap<>();
      
      Ar<ENVBlock> blockPalette = new Ar<>();
      ObjectIntMap<ENVBlock> blockMap = new ObjectIntMap<>();

      floorPalette.add((Floor)null); 
      blockPalette.add((ENVBlock)null);
      // floorMap.put(null, 0);
      // blockMap.put(null, 0);

      int total = WorldData.world.W * WorldData.world.H;

      // 第一遍扫描：统计用到了哪些方块
      for (int i = 0; i < total; i++) {
        Floor floor = WorldData.world.floors.get(i);
        ENVBlock block = WorldData.world.envblocks.get(i);

        // 只处理非空对象
        if (floor != null && !floorMap.containsKey(floor)) {
          floorMap.put(floor, floorPalette.size);
          floorPalette.add(floor);
        }
        
        if (block != null && !blockMap.containsKey(block)) {
          blockMap.put(block, blockPalette.size);
          blockPalette.add(block);
        }
      }
      
      w.s(floorPalette.size); 
      for (int i = 0; i < floorPalette.size; i++) {
          Floor f = floorPalette.get(i);
          w.str(f == null ? "null" : f.getLName());
      }

      w.s(blockPalette.size);
      for (int i = 0; i < blockPalette.size; i++) {
          ENVBlock b = blockPalette.get(i);
          w.str(b == null ? "null" : b.getLName());
      }

      for (int i = 0; i < total; i++) {
        Floor floor = WorldData.world.floors.get(i);
        ENVBlock block = WorldData.world.envblocks.get(i);

        w.s(floor == null ? 0 : floorMap.get(floor)); 
        w.s(block == null ? 0 : blockMap.get(block)); 
      }

      // 6. Units
      w.i(WorldData.units.size);
      for (Unit u : WorldData.units) {
        w.str(u.type.getLName());
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
      String magic = new String(r.b(8));
      if (!magic.equals(MAGIC)) throw new IOException("Invalid file format");

      int ver = r.i();
      int width = r.i();
      int height = r.i();

      // Tags
      int tagCount = r.s();
      for (int i = 0; i < tagCount; i++) { r.str(); r.str(); }

      WorldData.reBuildAll(width, height);
      //这里会自动处理导航数据
      
      // 读取地板映射表
      int floorPaletteSize = r.s();
      Floor[] floorLookup = new Floor[floorPaletteSize];
      for(int i=0; i<floorPaletteSize; i++){
          String name = r.str();
          floorLookup[i] = name.equals("null") ? null : ContentVar.get(name, Floor.class);
      }

      // 读取环境块映射表
      int blockPaletteSize = r.s();
      ENVBlock[] blockLookup = new ENVBlock[blockPaletteSize];
      for(int i=0; i<blockPaletteSize; i++){
          String name = r.str();
          blockLookup[i] = name.equals("null") ? null : ContentVar.get(name, ENVBlock.class);
      }

      int total = width * height;
      for (int i = 0; i < total; i++) {
        short floorId = r.s();
        short blockId = r.s();

        // 查表获取真实对象
        Floor floor = (floorId >= 0 && floorId < floorLookup.length) ? floorLookup[floorId] : null;
        ENVBlock block = (blockId >= 0 && blockId < blockLookup.length) ? blockLookup[blockId] : null;

        WorldData.world.floors.add(floor);
        WorldData.world.envblocks.add(block);
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
      RouteData.init();
      

    } catch (IOException e) {
      Log.err("Load failed", e);
      WorldData.initWorld();
    }
  }
}