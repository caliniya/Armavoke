package caliniya.armavoke.game.data;

import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.PQueue; // 使用 Arc 的 PQueue
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.world.ENVBlock;
import caliniya.armavoke.world.World;

public class RouteData {

  // 预计算的最大单位半径 (0, 1, 2, 3, 4)
  // 对应实际格数宽度: 1, 3, 5, 7, 9
  private static final int MAX_PRECALC_RADIUS = 4;

  // 最大支持的跨越能力等级 (0, 1, 2)
  private static final int MAX_CAPABILITY = 2;

  public static int W, H;

  // [Capability] -> Layer
  public static NavLayer[] layers;

  private RouteData() {}

  /** 导航层：包含特定跨越能力的障碍数据 */
  public static class NavLayer {
    public boolean[] baseSolidMap; // 基础障碍物图 (可能经过腐蚀)
    public int[] clearanceMap; // 距离场 (Brushfire 生成)

    // [Radius][Index] -> 是否不可通行
    // 预计算的膨胀障碍图：sizeMaps[r][i] == true 表示对于半径r的单位，i位置是墙
    public boolean[][] sizeMaps;

    public NavLayer(int size) {
      baseSolidMap = new boolean[size];
      clearanceMap = new int[size];
      sizeMaps = new boolean[MAX_PRECALC_RADIUS + 1][size];
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
    for (int i = 0; i < size; i++) {
      layers[0].baseSolidMap[i] = world.isSolid(i);
    }

    // 2. 初始化 Layer 1 ~ MAX (腐蚀层，用于机甲)
    for (int cap = 1; cap <= MAX_CAPABILITY; cap++) {
      layers[cap] = new NavLayer(size);
      erodeMap(layers[cap - 1].baseSolidMap, layers[cap].baseSolidMap);
    }

    // 3. 后处理每一层：计算距离场 + 生成体积膨胀图
    for (int cap = 0; cap <= MAX_CAPABILITY; cap++) {
      NavLayer layer = layers[cap];

      // A. 计算距离场
      calcClearance(layer);

      // B. 预计算体积膨胀图
      for (int r = 0; r <= MAX_PRECALC_RADIUS; r++) {
        for (int i = 0; i < size; i++) {
          // 核心逻辑：如果离最近障碍物的距离 <= 半径，则视为障碍
          // 例如半径为0(1x1)，需要 clearance > 0 (即 clearance >= 1)，即非墙
          // 例如半径为1(3x3)，需要 clearance > 1 (即离墙至少2格远)
          layer.sizeMaps[r][i] = layer.clearanceMap[i] <= r;
        }
      }
    }
  }

  // --- 核心算法部分 ---

  public static Ar<Point2> findPath(int sx, int sy, int tx, int ty, int unitSize, int capability) {
    capability = Mathf.clamp(capability, 0, MAX_CAPABILITY);
    NavLayer layer = layers[capability];

    // 检查终点可行性
    if (!isPassable(layer, tx, ty, unitSize)) return null;

    // JPS 初始化
    PQueue<Node> openList = new PQueue<>();
    boolean[] closedMap = new boolean[W * H];
    Node[] nodeIndex = new Node[W * H];

    Node startNode = new Node(sx, sy, null, 0, dist(sx, sy, tx, ty));
    openList.add(startNode);
    nodeIndex[coordToIndex(sx, sy)] = startNode;

    while (!openList.empty()) {
      Node current = openList.poll();
      int cIndex = coordToIndex(current.x, current.y);

      // Lazy Deletion
      if (closedMap[cIndex]) continue;
      if (nodeIndex[cIndex] != null && current != nodeIndex[cIndex]) continue;

      if (current.x == tx && current.y == ty) {
        return reconstructPath(current);
      }

      closedMap[cIndex] = true;

      identifySuccessors(layer, current, tx, ty, openList, closedMap, nodeIndex, unitSize);
    }

    return null;
  }

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

        if (existingNode == null || g < existingNode.g) {
          Node newNode = new Node(jx, jy, current, g, dist(jx, jy, tx, ty));
          nodeIndex[index] = newNode;
          openList.add(newNode);
        }
      }
    }
  }

  private static int[] getPrunedNeighbors(NavLayer layer, Node node, int unitSize) {
    if (node.parent == null) {
      return new int[] {0, 1, 0, -1, -1, 0, 1, 0, 1, 1, 1, -1, -1, 1, -1, -1};
    }

    int dx = Integer.compare(node.x - node.parent.x, 0);
    int dy = Integer.compare(node.y - node.parent.y, 0);

    // 这里的 isPassable 已经非常快了(查表)，所以可以放心在剪枝逻辑中调用
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
        }
        if (forcedTop) {
          res[c++] = dx;
          res[c++] = -dy;
        }

        int[] ret = new int[c];
        System.arraycopy(res, 0, ret, 0, c);
        return ret;
      }
    }
    return new int[] {};
  }

  private static Point2 jump(
      NavLayer layer, int cx, int cy, int dx, int dy, int tx, int ty, int unitSize) {
    int nx = cx + dx;
    int ny = cy + dy;

    if (!isPassable(layer, nx, ny, unitSize)) return null;
    if (nx == tx && ny == ty) return new Point2(nx, ny);

    if (dx != 0 && dy != 0) { // 对角线
      if ((!isPassable(layer, nx - dx, ny, unitSize)
              && isPassable(layer, nx - dx, ny + dy, unitSize))
          || (!isPassable(layer, nx, ny - dy, unitSize)
              && isPassable(layer, nx + dx, ny - dy, unitSize))) {
        return new Point2(nx, ny);
      }
      if (jump(layer, nx, ny, dx, 0, tx, ty, unitSize) != null
          || jump(layer, nx, ny, 0, dy, tx, ty, unitSize) != null) {
        return new Point2(nx, ny);
      }
    } else { // 直线
      if (dx != 0) {
        if ((!isPassable(layer, nx, ny - 1, unitSize)
                && isPassable(layer, nx + dx, ny - 1, unitSize))
            || (!isPassable(layer, nx, ny + 1, unitSize)
                && isPassable(layer, nx + dx, ny + 1, unitSize))) {
          return new Point2(nx, ny);
        }
      } else {
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

  /** 判断是否可通过 优先查预计算的 sizeMap，如果是超大单位则回退到查 clearanceMap */
  public static boolean isPassable(NavLayer layer, int x, int y, int unitSize) {
    if (!isValid(x, y)) return false;
    int index = coordToIndex(x, y);

    // 【核心优化】
    if (unitSize <= MAX_PRECALC_RADIUS) {
      // 直接查预计算好的 boolean 数组，速度极快
      // 注意 sizeMaps 存的是"是否阻挡"，所以取反
      return !layer.sizeMaps[unitSize][index];
    } else {
      // 对于超大单位，回退到动态比较
      return layer.clearanceMap[index] > unitSize;
    }
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
      if (isValid(x + 1, y) && !src[coordToIndex(x + 1, y)]) isEdge = true;
      else if (isValid(x - 1, y) && !src[coordToIndex(x - 1, y)]) isEdge = true;
      else if (isValid(x, y + 1) && !src[coordToIndex(x, y + 1)]) isEdge = true;
      else if (isValid(x, y - 1) && !src[coordToIndex(x, y - 1)]) isEdge = true;
      dst[i] = !isEdge;
    }
  }

  /** 计算距离场 */
  private static void calcClearance(NavLayer layer) {
    // 这里的 layer.baseSolidMap 是该层的基础障碍图
    for (int i = 0; i < W * H; i++) {
      layer.clearanceMap[i] = layer.baseSolidMap[i] ? 0 : 9999;
    }
    // Pass 1
    for (int y = 0; y < H; y++) {
      for (int x = 0; x < W; x++) {
        if (layer.baseSolidMap[coordToIndex(x, y)]) continue;
        int v = layer.clearanceMap[coordToIndex(x, y)];
        if (isValid(x - 1, y)) v = Math.min(v, layer.clearanceMap[coordToIndex(x - 1, y)] + 1);
        if (isValid(x, y - 1)) v = Math.min(v, layer.clearanceMap[coordToIndex(x, y - 1)] + 1);
        layer.clearanceMap[coordToIndex(x, y)] = v;
      }
    }
    // Pass 2
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
