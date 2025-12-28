package caliniya.armavoke.game.data;

import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.IntMap;
import arc.struct.PQueue;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.world.ENVBlock;
import caliniya.armavoke.world.World;

public class RouteData {

  // 最大支持的跨越能力等级 (0, 1, 2)
  private static final int MAX_CAPABILITY = 2;

  public static int W, H;

  // 每一层代表一种跨越能力
  public static NavLayer[] layers;

  private RouteData() {}

  /** 导航层：包含特定跨越能力的障碍数据和距离场 */
  public static class NavLayer {
    public boolean[] solidMap; // 障碍物图 (可能经过腐蚀)
    public int[] clearanceMap; // 距离场 (Brushfire 生成)

    public NavLayer(int size) {
      solidMap = new boolean[size];
      clearanceMap = new int[size];
    }
  }

  public static void init() {
    World world = WorldData.world;
    W = world.W;
    H = world.H;
    int size = W * H;

    layers = new NavLayer[MAX_CAPABILITY + 1];

    // 1. 初始化 Layer 0 (原始地图)
    layers[0] = new NavLayer(size);
    
    // 【优化】统一使用 world.isSolid(i) 接口
    for (int i = 0; i < size; i++) {
        // 如果 World 认为这里是障碍物，那就是障碍物
        layers[0].solidMap[i] = world.isSolid(i);
    }

    // 2. 初始化 Layer 1 ~ MAX (腐蚀层，用于机甲)
    for (int cap = 1; cap <= MAX_CAPABILITY; cap++) {
      layers[cap] = new NavLayer(size);
      erodeMap(layers[cap - 1].solidMap, layers[cap].solidMap);
    }

    // 3. 为每一层计算距离场
    for (int cap = 0; cap <= MAX_CAPABILITY; cap++) {
      calcClearance(layers[cap]);
    }
  }

  // --- 核心算法部分 ---

  /**
   * JPS 寻路入口
   *
   * @param unitSize 单位半径 (0=1x1, 1=3x3)
   * @param capability 跨越能力 (0=普通, 1=机甲)
   */
  public static Ar<Point2> findPath(int sx, int sy, int tx, int ty, int unitSize, int capability) {
    capability = Mathf.clamp(capability, 0, MAX_CAPABILITY);
    NavLayer layer = layers[capability];

    if (!isPassable(layer, tx, ty, unitSize)) return null;

    // 使用 Arc 的 PQueue，由于 Node 实现了 Comparable，不需要 Comparator
    PQueue<Node> openList = new PQueue<>();
    boolean[] closedMap = new boolean[W * H];
    // nodeIndex 存储的是我们目前发现的最优节点
    Node[] nodeIndex = new Node[W * H];

    Node startNode = new Node(sx, sy, null, 0, dist(sx, sy, tx, ty));
    openList.add(startNode);
    nodeIndex[coordToIndex(sx, sy)] = startNode;

    while (!openList.empty()) {
      Node current = openList.poll();
      // 【关键修改：Lazy Deletion】
      // 如果这个节点已经在 closedMap 里，或者是旧的劣质节点（g值比记录的要大），丢弃
      int cIndex = coordToIndex(current.x, current.y);

      // 检查1：是否已经关闭
      if (closedMap[cIndex]) continue;

      // 检查2：是否是僵尸节点 (即我们已经找到了一条更短的路到达这里)
      // 注意：因为我们只在发现更短路径时更新 nodeIndex，所以只需比较对象引用
      if (nodeIndex[cIndex] != null && current != nodeIndex[cIndex]) continue;
      if (current.x == tx && current.y == ty) {
        return reconstructPath(current);
      }

      closedMap[cIndex] = true;

      identifySuccessors(layer, current, tx, ty, openList, closedMap, nodeIndex, unitSize);
    }

    return null;
  }

  /** JPS: 识别后继节点 (Pruning + Jumping) */
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

      Point2 jp = jump(layer, current.x, current.y, dx, dy, tx, ty, unitSize);

      if (jp != null) {
        int jx = (int) jp.x;
        int jy = (int) jp.y;
        int index = coordToIndex(jx, jy);
        if (closedMap[index]) continue;
        float g = current.g + dist(current.x, current.y, jx, jy);
        Node existingNode = nodeIndex[index];
        // 【关键修改：不使用 remove/update】
        // 如果发现更短路径，直接 new 一个新 Node 并 add 到堆中
        if (existingNode == null || g < existingNode.g) {
          Node newNode = new Node(jx, jy, current, g, dist(jx, jy, tx, ty));
          nodeIndex[index] = newNode; // 更新最优节点记录
          openList.add(newNode); // 直接添加新节点
          // 旧的 existingNode 依然在堆里，但稍后会被 Lazy Deletion 机制丢弃
        }
      }
    }
  }

  /** JPS: 获取剪枝后的邻居方向 */
  private static int[] getPrunedNeighbors(NavLayer layer, Node node, int unitSize) {
    if (node.parent == null) {
      // 起点：返回所有 8 方向
      return new int[] {0, 1, 0, -1, -1, 0, 1, 0, 1, 1, 1, -1, -1, 1, -1, -1};
    }

    int px = node.parent.x;
    int py = node.parent.y;
    int dx = Integer.compare(node.x - px, 0);
    int dy = Integer.compare(node.y - py, 0);

    // 动态列表，最多5个方向
    // 这里为了性能，可以用 IntSeq 或者定长数组+计数器
    // 简单起见，这里罗列逻辑

    // 直线移动 (dx!=0, dy=0) 或 (dx=0, dy!=0)
    if (dx != 0 && dy == 0) { // 水平
      if (isPassable(layer, node.x + dx, node.y, unitSize)) {
        // 前方
        // 强制邻居检测：如果上方阻挡且右上方通行 -> 强制去右上方
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
      return new int[] {};
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
      return new int[] {};
    } else { // 对角线 (dx!=0, dy!=0)
      boolean nextPass = isPassable(layer, node.x + dx, node.y + dy, unitSize);
      boolean hPass = isPassable(layer, node.x + dx, node.y, unitSize);
      boolean vPass = isPassable(layer, node.x, node.y + dy, unitSize);

      if (nextPass && (hPass || vPass)) { // 只要有一个分量通，对角线就通 (假设允许切角)
        boolean forcedH = !vPass && hPass; // 垂直堵了，但水平通，可能产生水平分量的强制邻居？JPS 逻辑略复杂
        // 标准 JPS 对角线剪枝：保留 前方(dx,dy), 水平(dx,0), 垂直(0,dy)
        // 强制邻居：
        boolean forcedLeft =
            !isPassable(layer, node.x - dx, node.y, unitSize)
                && isPassable(layer, node.x - dx, node.y + dy, unitSize); // 背后的水平墙
        boolean forcedTop =
            !isPassable(layer, node.x, node.y - dy, unitSize)
                && isPassable(layer, node.x + dx, node.y - dy, unitSize); // 背后的垂直墙

        // 构造返回列表
        // 这里为了简化代码，返回一个包含所有可能性的数组，可能会有多余检查但不会错
        int[] res = new int[10];
        int c = 0;
        res[c++] = dx;
        res[c++] = dy;
        res[c++] = dx;
        res[c++] = 0;
        res[c++] = 0;
        res[c++] = dy;
        if (forcedLeft) {
          res[c++] = -dx;
          res[c++] = dy;
        } // 90度拐弯
        if (forcedTop) {
          res[c++] = dx;
          res[c++] = -dy;
        }

        // 截断数组
        int[] ret = new int[c];
        System.arraycopy(res, 0, ret, 0, c);
        return ret;
      }
    }
    return new int[] {};
  }

  /** JPS: 递归跳跃 */
  private static Point2 jump(
      NavLayer layer, int cx, int cy, int dx, int dy, int tx, int ty, int unitSize) {
    int nx = cx + dx;
    int ny = cy + dy;

    if (!isPassable(layer, nx, ny, unitSize)) return null;
    if (nx == tx && ny == ty) return new Point2(nx, ny);

    // 检查强制邻居 (Forced Neighbors)
    if (dx != 0 && dy != 0) { // 对角线
      // 对角线移动时，如果水平分量被阻挡但水平前一步是通的 -> 强制
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
      if (dx != 0) { // 水平
        if ((!isPassable(layer, nx, ny - 1, unitSize)
                && isPassable(layer, nx + dx, ny - 1, unitSize))
            || (!isPassable(layer, nx, ny + 1, unitSize)
                && isPassable(layer, nx + dx, ny + 1, unitSize))) {
          return new Point2(nx, ny);
        }
      } else { // 垂直
        if ((!isPassable(layer, nx - 1, ny, unitSize)
                && isPassable(layer, nx - 1, ny + dy, unitSize))
            || (!isPassable(layer, nx + 1, ny, unitSize)
                && isPassable(layer, nx + 1, ny + dy, unitSize))) {
          return new Point2(nx, ny);
        }
      }
    }

    return jump(layer, nx, ny, dx, dy, tx, ty, unitSize);
  }

  // --- 工具方法 ---

  /** 判断是否可通过：结合了 Solid Map 和 Clearance Map */
  public static boolean isPassable(NavLayer layer, int x, int y, int unitSize) {
    if (!isValid(x, y)) return false;
    // 如果是墙，clearance=0，肯定 <= unitSize (unitSize最小是0)
    // 所以直接查 clearance 即可
    return layer.clearanceMap[coordToIndex(x, y)] > unitSize;
  }

  /** 地图腐蚀 */
  private static void erodeMap(boolean[] src, boolean[] dst) {
    for (int i = 0; i < W * H; i++) {
      if (!src[i]) {
        dst[i] = false;
        continue;
      }
      int x = i % W;
      int y = i / W;
      boolean isEdge = false;
      // 如果四周有空地，则当前墙壁被腐蚀掉
      if (isValid(x + 1, y) && !src[coordToIndex(x + 1, y)]) isEdge = true;
      else if (isValid(x - 1, y) && !src[coordToIndex(x - 1, y)]) isEdge = true;
      else if (isValid(x, y + 1) && !src[coordToIndex(x, y + 1)]) isEdge = true;
      else if (isValid(x, y - 1) && !src[coordToIndex(x, y - 1)]) isEdge = true;
      dst[i] = !isEdge;
    }
  }

  /** 计算距离场 */
  private static void calcClearance(NavLayer layer) {
    for (int i = 0; i < W * H; i++) {
      layer.clearanceMap[i] = layer.solidMap[i] ? 0 : 9999;
    }
    for (int y = 0; y < H; y++) {
      for (int x = 0; x < W; x++) {
        if (layer.solidMap[coordToIndex(x, y)]) continue;
        int v = layer.clearanceMap[coordToIndex(x, y)];
        if (isValid(x - 1, y)) v = Math.min(v, layer.clearanceMap[coordToIndex(x - 1, y)] + 1);
        if (isValid(x, y - 1)) v = Math.min(v, layer.clearanceMap[coordToIndex(x, y - 1)] + 1);
        layer.clearanceMap[coordToIndex(x, y)] = v;
      }
    }
    for (int y = H - 1; y >= 0; y--) {
      for (int x = W - 1; x >= 0; x--) {
        if (layer.solidMap[coordToIndex(x, y)]) continue;
        int v = layer.clearanceMap[coordToIndex(x, y)];
        if (isValid(x + 1, y)) v = Math.min(v, layer.clearanceMap[coordToIndex(x + 1, y)] + 1);
        if (isValid(x, y + 1)) v = Math.min(v, layer.clearanceMap[coordToIndex(x, y + 1)] + 1);
        layer.clearanceMap[coordToIndex(x, y)] = v;
      }
    }
  }

  // ... 其他 helper (isValid, coordToIndex, dist, reconstructPath, Node) ...
  public static boolean isValid(int x, int y) {
    return x >= 0 && x < W && y >= 0 && y < H;
  }

  public static int coordToIndex(int x, int y) {
    return y * W + x;
  }

  private static float dist(int x1, int y1, int x2, int y2) {
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
