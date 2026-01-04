package caliniya.armavoke.game.data;

import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.IntQueue;
import arc.struct.PQueue;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.world.ENVBlock;
import caliniya.armavoke.world.World;

public class RouteData {

  // 预计算的最大单位半径 (0, 1, 2, 3, 4)
  public static final int MAX_PRECALC_RADIUS = 4;
  // 最大支持的跨越能力等级
  public static final int MAX_CAPABILITY = 2;

  // 局部更新时，重算距离场的最大范围
  private static final int UPDATE_RANGE = 24;
  private static final int MAX_DIST_VAL = 9999;

  public static int W, H;
  public static NavLayer[] layers;

  // 用于增量更新的锁
  public static final Object updateLock = new Object();

  private RouteData() {}

  /** 内部类：导航层数据 */
  public static class NavLayer {
    public boolean[] baseSolidMap; // 基础障碍物 (是否是墙)
    public int[] clearanceMap; // 距离场
    public boolean[][] sizeMaps; // [Radius][Index] 体积阻挡缓存

    public NavLayer(int size) {
      baseSolidMap = new boolean[size];
      clearanceMap = new int[size];
      sizeMaps = new boolean[MAX_PRECALC_RADIUS + 1][size];
    }
  }

  /** 初始化全图数据 */
  public static void init() {
    World world = WorldData.world;
    W = world.W;
    H = world.H;
    int size = W * H;

    layers = new NavLayer[MAX_CAPABILITY + 1];

    // 1. 初始化 Layer 0
    layers[0] = new NavLayer(size);
    for (int i = 0; i < size; i++) {
      layers[0].baseSolidMap[i] = world.isSolid(i);
    }

    // 2. 初始化腐蚀层 (Layer 1 ~ MAX)
    for (int cap = 1; cap <= MAX_CAPABILITY; cap++) {
      layers[cap] = new NavLayer(size);
      erodeMapFull(layers[cap - 1].baseSolidMap, layers[cap].baseSolidMap);
    }

    // 3. 计算距离场和体积图
    for (int cap = 0; cap <= MAX_CAPABILITY; cap++) {
      calcClearanceFull(layers[cap]);
      updateSizeMapsFull(layers[cap]);
    }
  }

  /**动态更新某个方块的状态 自动处理级联腐蚀和局部距离场重算 */
  public static void updateBlock(int x, int y, boolean isSolid) {
    synchronized (updateLock) {
      if (!isValid(x, y)) return;

      // 1. 更新 Layer 0 基础数据
      NavLayer l0 = layers[0];
      int index = coordToIndex(x, y);
      if (l0.baseSolidMap[index] == isSolid) return; // 状态没变
      l0.baseSolidMap[index] = isSolid;

      // 更新 Layer 0 的局部区域
      updateRegion(l0, x, y, x, y);

      // 2. 级联更新腐蚀层 (Layer 1 ~ MAX)
      // Layer 0 变动一点，Layer 1 的腐蚀状态可能影响周围 3x3，Layer 2 影响 5x5
      for (int cap = 1; cap <= MAX_CAPABILITY; cap++) {
        NavLayer prev = layers[cap - 1];
        NavLayer curr = layers[cap];

        // 腐蚀影响范围：每一层向外扩展 1 格
        int range = cap;
        int minX = Math.max(0, x - range);
        int maxX = Math.min(W - 1, x + range);
        int minY = Math.max(0, y - range);
        int maxY = Math.min(H - 1, y + range);

        // 局部重新计算腐蚀图
        boolean changed = false;
        for (int ry = minY; ry <= maxY; ry++) {
          for (int rx = minX; rx <= maxX; rx++) {
            boolean oldState = curr.baseSolidMap[coordToIndex(rx, ry)];
            boolean newState = calcErosionAt(prev.baseSolidMap, rx, ry);
            if (oldState != newState) {
              curr.baseSolidMap[coordToIndex(rx, ry)] = newState;
              changed = true;
            }
          }
        }

        // 如果这一层的墙壁分布变了，重算这一层的距离场
        if (changed) {
          // 更新范围需要比腐蚀范围大，以传播距离场变化
          updateRegion(curr, minX, minY, maxX, maxY);
        }
      }
    }
  }

  /**
   * 局部区域重算距离场 (Bounded BFS)
   *
   * @param minX, minY, maxX, maxY 定义了发生物理变化的区域
   */
  private static void updateRegion(NavLayer layer, int minX, int minY, int maxX, int maxY) {
    // 定义受影响的重算区域 (Update Box)
    // 向外扩展 UPDATE_RANGE，确保距离场数值能平滑过渡到区域外
    int uMinX = Math.max(0, minX - UPDATE_RANGE);
    int uMaxX = Math.min(W - 1, maxX + UPDATE_RANGE);
    int uMinY = Math.max(0, minY - UPDATE_RANGE);
    int uMaxY = Math.min(H - 1, maxY + UPDATE_RANGE);

    IntQueue queue = new IntQueue();

    // 1. 初始化重算区域
    for (int y = uMinY; y <= uMaxY; y++) {
      for (int x = uMinX; x <= uMaxX; x++) {
        int idx = coordToIndex(x, y);

        if (layer.baseSolidMap[idx]) {
          // 如果是墙，距离为0，作为种子放入队列
          layer.clearanceMap[idx] = 0;
          queue.addLast(idx);
        } else {
          // 如果是空地
          // A. 如果在区域边缘：保留其原有值作为"边界条件"种子
          boolean isBorder = (x == uMinX || x == uMaxX || y == uMinY || y == uMaxY);
          if (isBorder) {
            // 只有有效值才入队
            if (layer.clearanceMap[idx] < MAX_DIST_VAL) {
              queue.addLast(idx);
            }
          } else {
            // B. 如果在区域内部：重置为无穷大，等待 BFS 填值
            layer.clearanceMap[idx] = MAX_DIST_VAL;
          }
        }
      }
    }

    // 2. 局部 BFS 传播
    while (!queue.isEmpty()) {
      int curr = queue.removeFirst();
      int cVal = layer.clearanceMap[curr];

      // 超过范围的值没必要继续传播 (优化)
      if (cVal >= UPDATE_RANGE + 5) continue;

      int cx = curr % W;
      int cy = curr / W;

      // 检查 4 邻居
      if (cx > uMinX) checkAndPropagate(layer, curr - 1, cVal, queue);
      if (cx < uMaxX) checkAndPropagate(layer, curr + 1, cVal, queue);
      if (cy > uMinY) checkAndPropagate(layer, curr - W, cVal, queue);
      if (cy < uMaxY) checkAndPropagate(layer, curr + W, cVal, queue);
    }

    // 3. 同步更新 sizeMaps (仅更新受影响区域)
    for (int r = 0; r <= MAX_PRECALC_RADIUS; r++) {
      for (int y = uMinY; y <= uMaxY; y++) {
        for (int x = uMinX; x <= uMaxX; x++) {
          int i = coordToIndex(x, y);
          layer.sizeMaps[r][i] = layer.clearanceMap[i] <= r;
        }
      }
    }
  }

  private static void checkAndPropagate(
      NavLayer layer, int neighborIdx, int currentVal, IntQueue queue) {
    // 如果邻居可以通过当前格变得更近
    if (layer.clearanceMap[neighborIdx] > currentVal + 1) {
      layer.clearanceMap[neighborIdx] = currentVal + 1;
      queue.addLast(neighborIdx);
    }
  }

  /** 计算单点的腐蚀状态 */
  private static boolean calcErosionAt(boolean[] srcMap, int x, int y) {
    int idx = coordToIndex(x, y);
    if (!srcMap[idx]) return false; // 本来就是空的

    // 检查四周是否有空地
    if (isValid(x + 1, y) && !srcMap[coordToIndex(x + 1, y)]) return false;
    if (isValid(x - 1, y) && !srcMap[coordToIndex(x - 1, y)]) return false;
    if (isValid(x, y + 1) && !srcMap[coordToIndex(x, y + 1)]) return false;
    if (isValid(x, y - 1) && !srcMap[coordToIndex(x, y - 1)]) return false;

    return true; // 四周都是墙，保留
  }

  private static void erodeMapFull(boolean[] src, boolean[] dst) {
    for (int i = 0; i < W * H; i++) {
      dst[i] = calcErosionAt(src, i % W, i / W);
    }
  }

  private static void calcClearanceFull(NavLayer layer) {
    // 全量 Brushfire 初始化
    for (int i = 0; i < W * H; i++)
      layer.clearanceMap[i] = layer.baseSolidMap[i] ? 0 : MAX_DIST_VAL;

    for (int y = 0; y < H; y++) {
      for (int x = 0; x < W; x++) {
        if (layer.baseSolidMap[coordToIndex(x, y)]) continue;
        int v = layer.clearanceMap[coordToIndex(x, y)];
        if (isValid(x - 1, y)) v = Math.min(v, layer.clearanceMap[coordToIndex(x - 1, y)] + 1);
        if (isValid(x, y - 1)) v = Math.min(v, layer.clearanceMap[coordToIndex(x, y - 1)] + 1);
        layer.clearanceMap[coordToIndex(x, y)] = v;
      }
    }
    for (int y = H - 1; y >= 0; y--) {
      for (int x = W - 1; x >= 0; x--) {
        if (layer.baseSolidMap[coordToIndex(x, y)]) continue;
        int v = layer.clearanceMap[coordToIndex(x, y)];
        if (isValid(x + 1, y)) v = Math.min(v, layer.clearanceMap[coordToIndex(x + 1, y)] + 1);
        if (isValid(x, y + 1)) v = Math.min(v, layer.clearanceMap[coordToIndex(x, y + 1)] + 1);
        layer.clearanceMap[coordToIndex(x, y)] = v;
      }
    }
  }

  private static void updateSizeMapsFull(NavLayer layer) {
    for (int r = 0; r <= MAX_PRECALC_RADIUS; r++) {
      for (int i = 0; i < W * H; i++) {
        layer.sizeMaps[r][i] = layer.clearanceMap[i] <= r;
      }
    }
  }
  
  public static boolean isValid(int x, int y) {
    return x >= 0 && x < W && y >= 0 && y < H;
  }

  public static int coordToIndex(int x, int y) {
    return y * W + x;
  }

  // 调试用 Getter
  public static boolean[] getDebugSolidMap() {
    return layers != null ? layers[0].baseSolidMap : null;
  }

  public static int[] getDebugClearanceMap() {
    return layers != null ? layers[0].clearanceMap : null;
  }
  
  /**
   * 获取路径
   *
   * @param unitSize 单位半径 (0=1x1, 1=3x3...)
   * @param capability 跨越能力 (0=普通, 1=机甲...)
   */
  public static Ar<Point2> findPath(int sx, int sy, int tx, int ty, int unitSize, int capability) {
    // 加锁防止读取到正在更新的脏数据
    synchronized (updateLock) {
      capability = Mathf.clamp(capability, 0, MAX_CAPABILITY);
      NavLayer layer = layers[capability];

      // 1. 终点检查
      if (!isPassable(layer, tx, ty, unitSize)) return null;

      // 2. 初始化 A* 数据结构
      PQueue<Node> openList = new PQueue<>();
      boolean[] closedMap = new boolean[W * H];
      Node[] nodeIndex = new Node[W * H];

      Node startNode = new Node(sx, sy, null, 0, dist(sx, sy, tx, ty));
      openList.add(startNode);
      nodeIndex[coordToIndex(sx, sy)] = startNode;

      while (!openList.empty()) {
        Node current = openList.poll();
        int cIndex = coordToIndex(current.x, current.y);

        // Lazy Deletion: 如果节点已关闭或已被更新的节点取代，跳过
        if (closedMap[cIndex]) continue;
        if (nodeIndex[cIndex] != null && current != nodeIndex[cIndex]) continue;

        // 到达终点 -> 平滑路径并返回
        if (current.x == tx && current.y == ty) {
          return smoothPath(reconstructPath(current), layer, unitSize);
        }

        closedMap[cIndex] = true;

        // 扩展节点 (JPS 逻辑)
        identifySuccessors(layer, current, tx, ty, openList, closedMap, nodeIndex, unitSize);
      }

      return null;
    }
  }

  /** JPS: 识别并添加后继节点 */
  private static void identifySuccessors(
      NavLayer layer,
      Node current,
      int tx,
      int ty,
      PQueue<Node> openList,
      boolean[] closedMap,
      Node[] nodeIndex,
      int unitSize) {

    int[] dirs = getPrunedNeighbors(layer, current, unitSize);

    for (int i = 0; i < dirs.length; i += 2) {
      int dx = dirs[i];
      int dy = dirs[i + 1];

      // 尝试跳跃
      Point2 jp = jump(layer, current.x, current.y, dx, dy, tx, ty, unitSize);

      if (jp != null) {
        int jx = (int) jp.x;
        int jy = (int) jp.y;
        int index = coordToIndex(jx, jy);

        if (closedMap[index]) continue;

        float g = current.g + dist(current.x, current.y, jx, jy);
        Node existingNode = nodeIndex[index];

        // 如果发现了更优路径，添加到 openList (Lazy Deletion 模式)
        if (existingNode == null || g < existingNode.g) {
          Node newNode = new Node(jx, jy, current, g, dist(jx, jy, tx, ty));
          nodeIndex[index] = newNode;
          openList.add(newNode);
        }
      }
    }
  }

  /** JPS: 递归/迭代 跳跃检测 */
  private static Point2 jump(
      NavLayer layer, int cx, int cy, int dx, int dy, int tx, int ty, int unitSize) {

    int nx = cx + dx;
    int ny = cy + dy;

    // 基础检查：越界或不可通行
    if (!isPassable(layer, nx, ny, unitSize)) return null;
    // 到达终点
    if (nx == tx && ny == ty) return new Point2(nx, ny);

    // 强制邻居检测 (Forced Neighbor)
    if (dx != 0 && dy != 0) { // 对角线
      if ((!isPassable(layer, nx - dx, ny, unitSize)
              && isPassable(layer, nx - dx, ny + dy, unitSize))
          || (!isPassable(layer, nx, ny - dy, unitSize)
              && isPassable(layer, nx + dx, ny - dy, unitSize))) {
        return new Point2(nx, ny);
      }
      // 递归检查水平和垂直分量
      if (jump(layer, nx, ny, dx, 0, tx, ty, unitSize) != null
          || jump(layer, nx, ny, 0, dy, tx, ty, unitSize) != null) {
        return new Point2(nx, ny);
      }
    } else { // 直线
      if (dx != 0) { // 水平移动
        if ((!isPassable(layer, nx, ny - 1, unitSize)
                && isPassable(layer, nx + dx, ny - 1, unitSize))
            || (!isPassable(layer, nx, ny + 1, unitSize)
                && isPassable(layer, nx + dx, ny + 1, unitSize))) {
          return new Point2(nx, ny);
        }
      } else { // 垂直移动
        if ((!isPassable(layer, nx - 1, ny, unitSize)
                && isPassable(layer, nx - 1, ny + dy, unitSize))
            || (!isPassable(layer, nx + 1, ny, unitSize)
                && isPassable(layer, nx + 1, ny + dy, unitSize))) {
          return new Point2(nx, ny);
        }
      }
    }

    // 继续沿当前方向跳跃
    return jump(layer, nx, ny, dx, dy, tx, ty, unitSize);
  }

  /** JPS: 获取剪枝后的搜索方向 */
  private static int[] getPrunedNeighbors(NavLayer layer, Node node, int unitSize) {
    if (node.parent == null) {
      // 起点：返回所有8方向
      return new int[] {0, 1, 0, -1, -1, 0, 1, 0, 1, 1, 1, -1, -1, 1, -1, -1};
    }

    int dx = Integer.compare(node.x - node.parent.x, 0);
    int dy = Integer.compare(node.y - node.parent.y, 0);

    if (dx != 0 && dy == 0) { // 水平
      if (isPassable(layer, node.x + dx, node.y, unitSize)) {
        boolean forcedUp =
            !isPassable(layer, node.x, node.y - 1, unitSize)
                && isPassable(layer, node.x + dx, node.y - 1, unitSize);
        boolean forcedDown =
            !isPassable(layer, node.x, node.y + 1, unitSize)
                && isPassable(layer, node.x + dx, node.y + 1, unitSize);

        if (forcedUp && forcedDown) return new int[] {dx, 0, dx, -1, dx, 1};
        if (forcedUp) return new int[] {dx, 0, dx, -1};
        if (forcedDown) return new int[] {dx, 0, dx, 1};
        return new int[] {dx, 0};
      }
    } else if (dx == 0 && dy != 0) { // 垂直
      if (isPassable(layer, node.x, node.y + dy, unitSize)) {
        boolean forcedLeft =
            !isPassable(layer, node.x - 1, node.y, unitSize)
                && isPassable(layer, node.x - 1, node.y + dy, unitSize);
        boolean forcedRight =
            !isPassable(layer, node.x + 1, node.y, unitSize)
                && isPassable(layer, node.x + 1, node.y + dy, unitSize);

        if (forcedLeft && forcedRight) return new int[] {0, dy, -1, dy, 1, dy};
        if (forcedLeft) return new int[] {0, dy, -1, dy};
        if (forcedRight) return new int[] {0, dy, 1, dy};
        return new int[] {0, dy};
      }
    } else { // 对角线
      boolean nextPass = isPassable(layer, node.x + dx, node.y + dy, unitSize);
      boolean hPass = isPassable(layer, node.x + dx, node.y, unitSize);
      boolean vPass = isPassable(layer, node.x, node.y + dy, unitSize);

      if (nextPass && (hPass || vPass)) {
        boolean forcedLeft =
            !isPassable(layer, node.x - dx, node.y, unitSize)
                && isPassable(layer, node.x - dx, node.y + dy, unitSize);
        boolean forcedTop =
            !isPassable(layer, node.x, node.y - dy, unitSize)
                && isPassable(layer, node.x + dx, node.y - dy, unitSize);

        // 简单的列表构建，避免复杂数组操作
        // 顺序：前方，水平，垂直，以及可能的强制拐弯
        int[] temp = new int[10];
        int c = 0;
        temp[c++] = dx;
        temp[c++] = dy;
        temp[c++] = dx;
        temp[c++] = 0;
        temp[c++] = 0;
        temp[c++] = dy;
        if (forcedLeft) {
          temp[c++] = -dx;
          temp[c++] = dy;
        }
        if (forcedTop) {
          temp[c++] = dx;
          temp[c++] = -dy;
        }

        int[] res = new int[c];
        System.arraycopy(temp, 0, res, 0, c);
        return res;
      }
    }
    return new int[] {};
  }

  // --- 路径平滑与射线检测 ---

  private static Ar<Point2> smoothPath(Ar<Point2> path, NavLayer layer, int unitSize) {
    if (path.size <= 2) return path;

    Ar<Point2> smoothed = new Ar<>();
    smoothed.add(path.get(0));

    int inputIndex = 0;
    while (inputIndex < path.size - 1) {
      int nextIndex = inputIndex + 1;
      for (int i = path.size - 1; i > inputIndex + 1; i--) {
        Point2 start = path.get(inputIndex);
        Point2 end = path.get(i);
        if (lineCast(layer, (int) start.x, (int) start.y, (int) end.x, (int) end.y, unitSize)) {
          nextIndex = i;
          break;
        }
      }
      smoothed.add(path.get(nextIndex));
      inputIndex = nextIndex;
    }
    return smoothed;
  }

  private static boolean lineCast(NavLayer layer, int x0, int y0, int x1, int y1, int unitSize) {
    int dx = Math.abs(x1 - x0);
    int dy = Math.abs(y1 - y0);
    int sx = x0 < x1 ? 1 : -1;
    int sy = y0 < y1 ? 1 : -1;
    int err = dx - dy;
    int cx = x0;
    int cy = y0;

    while (true) {
      if (!isPassable(layer, cx, cy, unitSize)) return false;
      if (cx == x1 && cy == y1) break;
      int e2 = 2 * err;
      if (e2 > -dy) {
        err -= dy;
        cx += sx;
      }
      if (e2 < dx) {
        err += dx;
        cy += sy;
      }
    }
    return true;
  }

  // --- 基础辅助方法 ---

  public static boolean isPassable(NavLayer layer, int x, int y, int unitSize) {
    if (!isValid(x, y)) return false;
    int index = coordToIndex(x, y);
    // 如果半径在预计算范围内，直接查 sizeMap
    if (unitSize <= MAX_PRECALC_RADIUS) {
      // sizeMaps 存的是 isSolid，所以取反
      return !layer.sizeMaps[unitSize][index];
    } else {
      // 超大单位回退到 Clearance 检查
      return layer.clearanceMap[index] > unitSize;
    }
  }

  private static float dist(int x1, int y1, int x2, int y2) {
    // 使用曼哈顿距离作为启发式，符合4/8向移动特征
    return Math.abs(x1 - x2) + Math.abs(y1 - y2);
  }

  private static Ar<Point2> reconstructPath(Node current) {
    Ar<Point2> p = new Ar<>();
    while (current != null) {
      p.add(new Point2(current.x, current.y));
      current = current.parent;
    }
    p.reverse();
    return p;
  }

  // A* 节点类
  private static class Node implements Comparable<Node> {
    int x, y;
    Node parent;
    float g, h;

    public Node(int x, int y, Node parent, float g, float h) {
      this.x = x;
      this.y = y;
      this.parent = parent;
      this.g = g;
      this.h = h;
    }

    @Override
    public int compareTo(Node o) {
      return Float.compare(g + h, o.g + o.h);
    }
  }
}
