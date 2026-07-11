package caliniya.armavoke.system.world;

import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.world.Floor;
import arc.struct.ObjectIntMap;
import caliniya.armavoke.world.ENVBlock;
import caliniya.armavoke.io.*;
import arc.math.Mathf;
import arc.util.Log;
import caliniya.armavoke.game.Building;
import caliniya.armavoke.game.Unit;
import caliniya.armavoke.game.data.WorldData;
import caliniya.armavoke.system.System;
import caliniya.armavoke.type.Weapon;

/**
 * 实体处理系统，运行在独立线程（60TPS）。
 *
 * <p>在这里处理索敌锁定，为实体指定目标。 底层网格已通过 {@code TeamData.gridLock} 实现线程安全。 具体的开火逻辑在主线程运行。
 */
public class EntityProces extends System<EntityProces> {

  public volatile boolean task2 = false, task3 = false;

  public Ar<Floor> floorPalette;
  public ObjectIntMap<Floor> floorMap;
  public Ar<ENVBlock> blockPalette;
  public ObjectIntMap<ENVBlock> blockMap;

  @Override
  public EntityProces init() {
    return super.init(true);
  }

  @Override
  public void update() {
    // --- 单位处理 ---
    WorldData.units.each(
        u -> {
          if (u == null) return;
          if (u.health <= 0) {
            u.kill();
            return;
          }

          for (Weapon w : u.weapons) {

            float wx = u.x + w.type.x;
            float wy = u.y + w.type.y;

            if (w.rotate) {
              // 旋转武器（炮塔）：独立锁敌
              // 目标失效 或 超出射程 → 重搜
              if (w.target == null
                  || w.target.health <= 0
                  || Mathf.dst2(wx, wy, w.target.x, w.target.y) > w.type.range * w.type.range) {
                w.type.findTarget(w, wx, wy);
              }
            } else {
              // 固定武器：直接瞄准单位锁定的目标
              w.target = u.target;
            }
          }
          if (task3) {
            u.write(DataIO.w);
          }
          if (task2) {
            // 任务二写入调试版
            for (int y = 0; y < WorldData.world.H; y++) {
              for (int x = 0; x < WorldData.world.W; x++) {
                Floor floor = WorldData.world.getFloor(x, y);
                ENVBlock block = WorldData.world.getENVBlock(x, y);
                DataIO.w.s(floor == null ? 0 : floorMap.get(floor, 0));
                DataIO.w.s(block == null ? 0 : blockMap.get(block, 0));
              }
            }
          }
          if (task) {
            // 任务一 --- 准备调色板 ---
            floorPalette = new Ar<>();
            floorMap = new ObjectIntMap<>();
            blockPalette = new Ar<>();
            blockMap = new ObjectIntMap<>();

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
            }
          }
        });

    // --- 建筑处理 ---
    WorldData.buildings.each(
        b -> {
          if (b == null) return;
          if (b.health <= 0) {
            b.kill();
            return;
          }

          if (b.target == null || b.target.health <= 0) {
            b.target = b.block.findTarget(b);
          }
          if (task3) {
            b.write(DataIO.w);
            // 当这一步执行完的时候，向内存中的数据写入就完成了
            DataIO.data = DataIO.bos.toByteArray();
            DataIO.copyed = true;
            GameIO.save(DataIO.saveTarget);
          }
        });

    if (task3) {
      task3 = false;
    }

    if (task2) {
      task2 = false;
      task3 = true;
    }

    if (task) {
      task = false;
      task2 = true;
    }
  }
}
