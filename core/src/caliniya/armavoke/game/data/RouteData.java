package caliniya.armavoke.game.data;

import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.IntMap;
import arc.util.Log;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.world.ENVBlock;
import caliniya.armavoke.world.World;
import arc.struct.IntQueue;
import java.util.PriorityQueue;

public class RouteData {

  private static final int MAX_PRECALC_RADIUS = 4;//预生成的最大单元集
  private static final int MAX_CAPABILITY = 2;//预计生成的最大跨越能力
  private static final int MECH_BASE_RADIUS = 1;

  public static int W, H;
  
  public static boolean[] baseSolidMap;

  private static class NavLayer {
    boolean[] solidMap;//该层该点是否为障碍
    int[] clearanceMap;//该点的距离场
    boolean[] interestMap;

    public NavLayer() {
      solidMap = new boolean[W * H];
      clearanceMap = new int[W * H];
      interestMap = new boolean[W * H];
    }
  }

  private static IntMap<NavLayer> navLayers = new IntMap<>();

  private RouteData() {}

  public static void init() {
    World world = WorldData.world;
    W = world.W;
    H = world.H;

    navLayers.clear();

    baseSolidMap = new boolean[W * H];
    Ar<ENVBlock> blocks = world.envblocks;
    for (int i = 0; i < blocks.size; i++) {
      baseSolidMap[i] = (world.isSolid(world.indexToX(i) , world.indexToY(i)));
    }
    
    
    for (int r = 0; r <= MAX_PRECALC_RADIUS; r++) {
      Log.info(r);
      NavLayer layer = new NavLayer();
      layer.solidMap = baseSolidMap;
      calcClearance(layer);
      loadJPointsForRadius(layer, r);
      navLayers.put(r, layer);
    }

    boolean[] lastErodedMap = baseSolidMap;
    for (int cap = 1; cap <= MAX_CAPABILITY; cap++) {
      Log.info(cap);
      NavLayer layer = new NavLayer();
      layer.solidMap = new boolean[W * H];
      erodeMap(lastErodedMap, layer.solidMap);
      lastErodedMap = layer.solidMap;

      calcClearance(layer);
      loadJPointsForRadius(layer, MECH_BASE_RADIUS);

      navLayers.put(-cap, layer);
    }
  }

  private static void erodeMap(boolean[] source, boolean[] target) {
    for (int y = 0; y < H; y++) {
      for (int x = 0; x < W; x++) {
        int index = coordToIndex(x, y);
        if (!source[index]) {
          target[index] = false;
        } else {
          boolean isEdge = false;
          if (isValid(x + 1, y) && !source[coordToIndex(x + 1, y)]) isEdge = true;
          else if (isValid(x - 1, y) && !source[coordToIndex(x - 1, y)]) isEdge = true;
          else if (isValid(x, y + 1) && !source[coordToIndex(x, y + 1)]) isEdge = true;
          else if (isValid(x, y - 1) && !source[coordToIndex(x, y - 1)]) isEdge = true;
          target[index] = !isEdge;
        }
      }
    }
  }

  private static void calcClearance(NavLayer layer) {
    IntQueue queue = new IntQueue(W * H);
    int[] dist = layer.clearanceMap;
    boolean[] solid = layer.solidMap;

    // 1. 初始化
    for (int i = 0; i < W * H; i++) {
        if (solid[i]) {
            dist[i] = 0;
            queue.addFirst(i); // 将所有墙壁作为源点加入队列
        } else {
            dist[i] = Integer.MAX_VALUE;
        }
    }

    // 2. BFS 扩散
    // 定义8个方向
    int[] dirsX = {0, 0, -1, 1, -1, -1, 1, 1};
    int[] dirsY = {1, -1, 0, 0, 1, -1, 1, -1};

    while (!queue.isEmpty()) {
        int currentIdx = queue.removeFirst();
        int cx = currentIdx % W;
        int cy = currentIdx / W;
        int currentDist = dist[currentIdx];

        // 向8个方向扩散
        for (int i = 0; i < 8; i++) {
            int nx = cx + dirsX[i];
            int ny = cy + dirsY[i];

            if (isValid(nx, ny)) {
                int nextIdx = coordToIndex(nx, ny);
                // 如果找到了更短的路径
                if (dist[nextIdx] > currentDist + 1) {
                    dist[nextIdx] = currentDist + 1;
                    queue.addFirst(nextIdx);
                }
            }
        }
    }
  }
  private static void loadJPointsForRadius(NavLayer layer, int radius) {
    for (int y = 0; y < H; y++) {
      for (int x = 0; x < W; x++) {
        if (hasForcedNeighbor(layer, x, y, radius)) {
          layer.interestMap[coordToIndex(x, y)] = true;
        }
      }
    }
  }

  public static Ar<Point2> findPath(int sx, int sy, int tx, int ty, float rawSize, int capability) {
    int radius = Mathf.ceil(rawSize);
    capability = Mathf.clamp(capability, 0, MAX_CAPABILITY);

    NavLayer layer;
    if (capability > 0) {
      layer = navLayers.get(-capability);
    } else {
      layer = navLayers.get(radius);
    }

    if (layer == null) return new Ar<>();

    int checkRadius = (capability > 0) ? Math.max(radius, MECH_BASE_RADIUS) : radius;
    
    // 【修复 2.1】增加起点检查
    if (!isPassable(layer, sx, sy, checkRadius)) {
        // Log.info("Path failed: Start point is not passable for this unit size.");
        return new Ar<>();
    }

    // 【修复 2.2】改进终点检查
    if (!isPassable(layer, tx, ty, checkRadius)) {
        // 目标点不可达，尝试寻找最近的可达点
        Point2 nearest = findNearestPassable(layer, tx, ty, checkRadius);
        if (nearest == null) {
            // Log.info("Path failed: No passable target found nearby.");
            return new Ar<>();
        }
        // 更新目标为最近的可达点
        tx = nearest.x;
        ty = nearest.y;
    }

    // ... 后续寻路逻辑保持不变 ...
    Ar<Point2> rawPath;
    if (capability == 0 && radius > MAX_PRECALC_RADIUS) {
      rawPath = aStarSearch(layer, sx, sy, tx, ty, radius);
    } else {
      rawPath = jpsSearchPrecalc(layer, sx, sy, tx, ty, checkRadius);
    }

    if (rawPath == null || rawPath.isEmpty()) return new Ar<>();
    //return smoothPath(rawPath, layer, checkRadius);
    return rawPath; // 先注释掉 smoothPath 方便调试
  }

  /** 新增辅助方法：寻找最近的可达点 (螺旋搜索) */
  private static Point2 findNearestPassable(NavLayer layer, int x, int y, int radius) {
      // 从 (x,y) 开始向外螺旋搜索
      int searchRadius = 1;
      int maxSearch = 10; // 最大搜索范围，防止无限循环
      
      while(searchRadius <= maxSearch) {
          for(int i = -searchRadius; i <= searchRadius; i++){
              for(int j = -searchRadius; j <= searchRadius; j++){
                  // 只搜索当前这一圈
                  if(Math.abs(i) != searchRadius && Math.abs(j) != searchRadius) continue;
                  
                  int nx = x + i;
                  int ny = y + j;
                  
                  if(isPassable(layer, nx, ny, radius)) {
                      return new Point2(nx, ny);
                  }
              }
          }
          searchRadius++;
      }
      return null;
  }
  private static Ar<Point2> jpsSearchPrecalc(
      NavLayer layer, int sx, int sy, int tx, int ty, int radius) {
    PriorityQueue<Node> openList = new PriorityQueue<>();
    boolean[] closedMap = new boolean[W * H];
    Node[] nodeIndex = new Node[W * H];

    Node startNode = new Node(sx, sy, null, 0, dist(sx, sy, tx, ty));
    openList.add(startNode);
    nodeIndex[coordToIndex(sx, sy)] = startNode;

    while (!openList.isEmpty()) {
      Node current = openList.poll();
      if (current.x == tx && current.y == ty) return reconstructPath(current);
      closedMap[coordToIndex(current.x, current.y)] = true;
      identifySuccessorsJPS(layer, current, tx, ty, openList, closedMap, nodeIndex, radius);
    }
    return null;
  }

  private static Ar<Point2> aStarSearch(
      NavLayer layer, int sx, int sy, int tx, int ty, int radius) {
    PriorityQueue<Node> openList = new PriorityQueue<>();
    boolean[] closedMap = new boolean[W * H];
    Node[] nodeIndex = new Node[W * H];
    openList.add(new Node(sx, sy, null, 0, dist(sx, sy, tx, ty)));
    int[] dirsX = {0, 0, -1, 1}, dirsY = {1, -1, 0, 0};

    while (!openList.isEmpty()) {
      Node current = openList.poll();
      if (current.x == tx && current.y == ty) return reconstructPath(current);
      if (closedMap[coordToIndex(current.x, current.y)]) continue;
      closedMap[coordToIndex(current.x, current.y)] = true;

      for (int i = 0; i < 4; i++) {
        int nx = current.x + dirsX[i], ny = current.y + dirsY[i];
        if (isPassable(layer, nx, ny, radius)) {
          float newG = current.g + 1;
          int index = coordToIndex(nx, ny);
          if (closedMap[index]) continue;
          Node neighbor = nodeIndex[index];
          if (neighbor == null || newG < neighbor.g) {
            neighbor = new Node(nx, ny, current, newG, dist(nx, ny, tx, ty));
            nodeIndex[index] = neighbor;
            openList.add(neighbor);
          }
        }
      }
    }
    return null;
  }

  private static void identifySuccessorsJPS(
      NavLayer layer,
      Node current,
      int tx,
      int ty,
      PriorityQueue<Node> openList,
      boolean[] closedMap,
      Node[] nodeIndex,
      int radius) {
    int[] dirsX = {0, 0, -1, 1, -1, -1, 1, 1}, dirsY = {1, -1, 0, 0, 1, -1, 1, -1};
    for (int i = 0; i < 8; i++) {
      Point2 jumpPoint = jump(layer, current.x, current.y, dirsX[i], dirsY[i], tx, ty, radius);
      if (jumpPoint != null) {
        int jx = (int) jumpPoint.x, jy = (int) jumpPoint.y, index = coordToIndex(jx, jy);
        if (closedMap[index]) continue;
        float gScore = current.g + dist(current.x, current.y, jx, jy);
        Node neighbor = nodeIndex[index];
        if (neighbor == null) {
          nodeIndex[index] = neighbor = new Node(jx, jy, current, gScore, dist(jx, jy, tx, ty));
          openList.add(neighbor);
        } else if (gScore < neighbor.g) {
          neighbor.g = gScore;
          neighbor.parent = current;
          openList.remove(neighbor);
          openList.add(neighbor);
        }
      }
    }
  }

  private static Point2 jump(
      NavLayer layer, int cx, int cy, int dx, int dy, int tx, int ty, int radius) {
    int nextX = cx + dx, nextY = cy + dy;
    if (!isPassable(layer, nextX, nextY, radius)) return null;
    if (nextX == tx && nextY == ty) return new Point2(nextX, nextY);
    if (layer.interestMap[coordToIndex(nextX, nextY)]) return new Point2(nextX, nextY);
    if (dx != 0 && dy != 0) {
      if (jump(layer, nextX, nextY, dx, 0, tx, ty, radius) != null
          || jump(layer, nextX, nextY, 0, dy, tx, ty, radius) != null) {
        return new Point2(nextX, nextY);
      }
    }
    return jump(layer, nextX, nextY, dx, dy, tx, ty, radius);
  }

  private static Ar<Point2> smoothPath(Ar<Point2> path, NavLayer layer, int radius) {
    if (path.size <= 2) return path;
    Ar<Point2> smoothed = new Ar<>();
    smoothed.add(path.get(0));
    int inputIndex = 0;
    while (inputIndex < path.size - 1) {
      int nextIndex = inputIndex + 1;
      for (int i = path.size - 1; i > inputIndex + 1; i--) {
        Point2 start = path.get(inputIndex), end = path.get(i);
        if (lineCast(layer, (int) start.x, (int) start.y, (int) end.x, (int) end.y, radius)) {
          nextIndex = i;
          break;
        }
      }
      smoothed.add(path.get(nextIndex));
      inputIndex = nextIndex;
    }
    return smoothed;
  }

  private static boolean lineCast(NavLayer layer, int x0, int y0, int x1, int y1, int radius) {
    int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
    int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
    int err = dx - dy, cx = x0, cy = y0;
    while (true) {
      if (!isPassable(layer, cx, cy, radius)) return false;
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

  private static boolean isPassable(NavLayer layer, int x, int y, int radius) {
    if (!isValid(x, y)) return false;
    return layer.clearanceMap[coordToIndex(x, y)] > radius;
  }

  private static boolean hasForcedNeighbor(NavLayer layer, int x, int y, int radius) {
    if (!isPassable(layer, x, y, radius)) return false;
    for (int i = -1; i <= 1; i++) {
      for (int j = -1; j <= 1; j++) {
        if ((i == 0 && j == 0) || !isValid(x + i, y + j)) continue;
        if (!isPassable(layer, x + i, y + j, radius)) return true;
      }
    }
    return false;
  }

  public static int coordToIndex(int x, int y) {
    return y * W + x;
  }

  public static boolean isValid(int x, int y) {
    return x >= 0 && x < W && y >= 0 && y < H;
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
