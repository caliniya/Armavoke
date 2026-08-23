###一次大型计划规划

> 目标：一次大型推进，打通「战役/星图/指挥/索敌/建造生产/调试」的关键闭环。
> 原则：功能优先、复用现有系统框架与线程模型；动画/美术后置；未确认项先按“默认方案”落地并在文中标注。

---

## 1. 宇宙界面 IO 测试窗口

- **现状**：`UniverseFragment` 已有「保存星域 / 读档 / 进度测试 / 关闭」按钮组。
- **目标**：新增「IO 测试」按钮 → 打开新窗口，内含一组测试按钮：
  - 进度读写：保存进度 / 读取进度（`ProgressIO`）
  - 地图读写：保存当前地图 / 读取地图（`GameIO` + `DataIO`）
  - 星域读写：保存星域 / 读星域（`WorldIO`），复用现有按钮逻辑
  - 回退加载测试：模拟「数据目录无副本 → 内置地图」路径
- **设计**：独立窗口类（`windows/IoTestWindow.java`），按钮逐个触发对应 IO 并 `Log.info` 结果。
- **涉及**：`UniverseFragment`、新增 `IoTestWindow`。

## 2. UniverseInput 重构（适配星图结构）

- **现状**：`UniverseInput` 维护的是旧的「32px 网格选中」（`Universe.selectedX/Y`），与 `StarMap`/`StarNode` 无关。
- **目标**：改为**节点交互**：
  - 点击命中检测：屏幕坐标 → `universeCamera.unproject` → `StarMap.getNode` 附近查询 + 节点半径判定 → `Universe.selectedNode`
  - 点击面板：命中节点后弹出/刷新节点详情（名称/状态/进入按钮）；空白处清除选择
  - 悬停高亮：`mouseMoved` 更新 `hoverNode`
- **设计**：`UniverseInput` 重写（保留 EnterUV/ExitUV 暂停开关）；`Universe` 增加 `selectedNode`/`hoverNode` 静态字段；渲染在 `UniverseRender` 高亮选中/悬停节点。
- **涉及**：`UniverseInput`、`Universe`、`UniverseRender`、`UniverseFragment`。

## 3. 单位索敌（让固定武器开火）

- **现状**：`EntityProces`（后台 60TPS）只给**旋转武器**按射程找目标；固定武器用 `u.target`，但**全项目没有任何地方给 `u.target` 赋值** → 固定武器永远不开火。
- **目标**：单位级索敌：
  - 单位无有效目标（null/死亡/超出 `UnitType.scanDistance`）时，用 `Entities.closestEnemy` 找最近敌人 → `u.target`
  - **节流**：每次搜索间隔 0.25s（`Unit.scanCooldown`），避免全量扫描
  - 停火状态（见第 10 项）不索敌；驻守状态照常索敌（原地攻击）
- **涉及**：`EntityProces`、`Unit`（新字段 `scanCooldown`）、`UnitType.scanDistance`（已有，默认 200）。

## 4. DebugFragment 优化（渲染调试器不动）

- **现状**：只显示 FPS/Mem/单位数/地图尺寸/Java/Android；有拼写错误 `Buinding`；每帧 `new StringBuilder`。
- **目标**：
  - 修正拼写，补 `Bullets` 计数
  - 扩展：各阵营实体数、`BulletProcess`/`EntityProces`/`UnitMath` 的实时 TPS（`System.tps`）、当前视图（菜单/星图/地图）、选中单位数与寻路中单位数
  - 复用 `StringBuilder` 字段（不再每帧 new）；信息分组、半透明背景
- **涉及**：`DebugFragment`。

## 5. 指挥：选中后单点不取消选中

- **现状**：点击已选单位会取消选中（`toggleUnitSelection` 是增删切换）。
- **目标**：单击**只增不删**；清空选中仅通过：①指挥面板「清空」按钮，②**关闭指挥模式（进入建造面板）自动清空**，③空框选。
- **设计**：`UnitControl.selectUnit` 只加入；`HUDFragment.updateRightPanel` 切换到建造面板（`commanding == false`）时调用 `clearSelection()`。
- **涉及**：`UnitControl`、`HUDFragment`。

## 6. 指挥：框选（覆盖式）

- **现状**：无框选。
- **目标**：框选矩形内的**我方单位**直接**覆盖**旧选中（不在框内的单位被移出选中）；**空框 = 快速清空**。
- **设计**：`CommandData.boxSelect` 开关；`UnitControl` 用 `pan/panStop` 手势绘制框（需返回 `true` 消费拖动，避免相机同时平移），`panStop` 用 `Core.camera.unproject` 把屏幕矩形转世界矩形，`WorldData.units.intersect` 收集我方单位 → 覆盖选中 → 刷新指挥面板。(ai-using有可以参考的文件)
- **触发方式**：见「待确认 Q1」。
- **涉及**：`UnitControl`、`CommandData`、`HUDFragment`（框选按钮）。

## 7. 指挥：正确实现阵营 + 可指挥敌人（移动攻击）

- **现状**：目前甚至可以直接指挥敌人。
- **目标**：
  - 指挥查询覆盖任意阵营：点击**我方**单位 → 选中；点击**敌方**单位 → 对有选中的单位下达**移动攻击**指令
  - 攻击目标与武器锁定分离：新增 `Unit.attackTarget`（移动目标），固定武器锁定仍是 `u.target`（自动索敌）
- **设计**：`UnitControl.findUnitAt`（任意阵营、半径 100）；`issueAttackCommand(enemy)`：选中单位设 `attackTarget`、清 `path`、从 `WorldData.moveunits` 移除；`Unit.update` 中 `attackTarget` 有效时直线接近至 `UnitType.engageRange`（默认 150px）后停下，同时朝向目标；目标死亡自动清除。
- **移动方式**：见「待确认 Q2」。
- **涉及**：`Unit`、`UnitType`、`UnitControl`。

## 8. 导航硬编码

- 暂不处理

## 9. 巡逻中动态墙 → 路径重算

- **现状**：`Unit.pathed = true` 后不再重算；目标点被新建墙堵住时单位卡死。
- **目标** : 如果目标不可达，可尝试提供一个移动到最近点的目标，放置方块的时候 对于影响到的路线 进行重算

## 10. 单位状态AI（驻守 / 战斗 / 停火）

- **目标** 一个具有状态的通用AI，当然前提是要实现一个AI控制器：
  - **驻守 `Guard`**：原地待命，攻击射程内敌人(未来在考虑制作追击范围内的敌人)；
  - **战斗 `Combat`**（默认）：遵循移动/攻击指令，自动攻击范围内敌人；下达移动/攻击指令时自动切回战斗
  - **停火 `HoldFire`**：可以移动，但不开火、不索敌
- **涉及**：`Unit`、`HUDFragment`、`UnitControl`、`EntityProces`。

## 11. 第一个物品 + 单位工厂 + 通用工厂菜单

- **现状**：物品已有 `Items.Ge`（germanium 锗 而且已经有了一个贴图但尚未使用）；建筑有 `TestBlock`（test-building，3×3）与 `testTurret`。
- **目标**：
  1. 完善第一个物品（germanium）：图标、显示名、可作为配方产物
  2. 第一个**单位工厂**：复用 `test-building` 贴图，继承现有 `Building`/`Block` 体系（新 `Factory extends Block`）
  3. 通用工厂菜单（点击自己的工厂打开）：
     - **配方列表**：每配方显示消耗（物品数）、生产时间、产物
     - **当前执行中配方**：进度显示
     - **物品列表**：`ItemModule` 内容
     - **电力状态 / 液体状态**：`PowerModule` / `LiquidModule` 当前容量
  4. 生产完成（暂不做动画）：在工厂前方（`angle` 朝向）生成单位
- **设计（草案）**：
  - 新增 `world/blocks/produce/unit/和world/blocks/produce/recipe`：`Recipe`（产物 `UnitType`、消耗 `ItemType+数量[]`、耗时秒）、`FactoryType`/`Factory`（Block 子类，持有配方数组 + 当前配方索引 + 生产进度）
  - `Building` 已有 `item/liquid/power` 模块 ✅；生产逻辑挂 `Factory.update`
  - 工厂菜单窗口：`factoryMenu`（点击工厂 → 打开），数据每帧/按 tick 刷新（复用 `LiveStatArea` 思路）
- **待确认**：见 Q3（产物单位、配方内容、菜单打开交互）。
- **涉及**：`Items`、`ItemType`、`Blocks`、新增 `Factory`，`Building`（小改），新增工厂菜单窗口。

## 12. 特效系统

- **现状**：无特效系统。
- **目标**：一套通用特效框架（发射体/粒子/贴图动画），供子弹命中、生产、死亡等使用。
- ai-using文件夹中有可参考的设计
- **涉及**：新增 `base/effect/`

---

## 排期与依赖

| 阶段 | 内容 | 依赖 |
|---|---|---|
| A（战斗基础） | 3、5、6、7、10、9 | 无（可并行） |
| B（星图与 IO） | 1、2 | 无 |
| C（生产玩法） | 11 | 建筑体系 |
| D（特效） | 12 | 等待参考文件 |
| E（打磨） | 4 | A 完成后（TPS 数据可用） |