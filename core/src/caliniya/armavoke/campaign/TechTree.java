package caliniya.armavoke.campaign;

import arc.util.io.Reads;
import arc.util.io.Writes;
import caliniya.armavoke.base.api.TechNodeContent;
import caliniya.armavoke.base.tool.Ar;

/**
 * 科技树。
 *
 * <p>采用手动构建：不同游戏阶段各自建立自己的科技树，
 * 通过 {@link #add(String)} 插入根节点、{@link #addChild(TechNode, String)}
 * 或 {@link TechNode#child(String)} 插入子节点。
 *
 * <p>解锁规则见 {@link TechNode#available()}：父节点解锁 + 自身前置满足。
 * 进度（unlocked/researched）是运行时状态，通过 write/read 序列化。
 */
public class TechTree {

  public final Ar<TechNode> nodes = new Ar<>();

  public TechTree() {}

  /** 插入一个根节点（纯策略节点）。 */
  public TechNode add(String name) {
    TechNode n = new TechNode(name);
    nodes.add(n);
    return n;
  }

  /** 插入一个关联内容的根节点。 */
  public TechNode add(TechNodeContent content) {
    TechNode n = new TechNode(content);
    nodes.add(n);
    return n;
  }

  /** 在指定节点下插入子节点，并把子节点登记到 nodes。 */
  public TechNode addChild(TechNode parent, String name) {
    TechNode n = parent.child(name);
    nodes.add(n);
    return n;
  }

  /** 按名称查找节点，不存在返回 null。 */
  public TechNode get(String name) {
    for (TechNode n : nodes) {
      if (n.name.equals(name)) return n;
    }
    return null;
  }

  /** 刷新解锁状态：从每个根节点开始沿树向下计算。 */
  public void updateUnlocks() {
    for (TechNode n : nodes) {
      if (n.parent == null) refresh(n);
    }
    // 兜底：未挂到树上的孤立节点
    for (TechNode n : nodes) {
      n.unlocked = n.available();
    }
  }

  private void refresh(TechNode n) {
    n.unlocked = n.available();
    for (TechNode c : n.children) {
      refresh(c);
    }
  }

  /** 研究一个节点；节点不存在或未解锁时返回 false。 */
  public boolean research(String name) {
    TechNode n = get(name);
    if (n == null) return false;
    boolean ok = n.research();
    updateUnlocks();
    return ok;
  }

  /** 序列化进度：节点数 + [名称, 研究状态] × n。结构由代码定义，这里只存进度。 */
  public void write(Writes w) {
    w.i(nodes.size);
    for (TechNode n : nodes) {
      w.str(n.name);
      w.bool(n.researched);
    }
  }

  /** 读入存档进度：按名称恢复研究状态（节点需已由代码构建）。 */
  public void read(Reads r) {
    int count = r.i();
    for (int i = 0; i < count; i++) {
      String name = r.str();
      boolean researched = r.bool();
      TechNode n = get(name);
      if (n != null) n.researched = researched;
    }
    updateUnlocks();
  }
}
