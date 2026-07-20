package caliniya.armavoke.base.tool;

import arc.func.*;
import arc.math.geom.*;
import arc.math.geom.QuadTree.*;
import arc.math.geom.*;

import arc.struct.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 实体组 —— 管理同一类型实体的集合。
 *
 * <p>功能:
 *
 * <ul>
 *   <li><b>平铺数组迭代</b> — O(n) 遍历所有实体，无序数组支持 O(1) 删除
 *   <li><b>四叉树空间查询</b> — O(log n + k) 矩形查询，自动维护空间索引
 *   <li><b>ID 映射（可选）</b> — O(1) 按 ID 查找实体
 * </ul>
 *
 * <p>
 *
 * @param <T> 实体类型，必须实现 {@link QuadTreeObject} 以提供碰撞盒
 */
@SuppressWarnings("unchecked")
public class EntityAr<T extends QuadTreeObject> implements Iterable<T> {

  private static final int DEFAULT_CAPACITY = 32;
  private static final int DEFAULT_ID_MAP_SIZE = 256;

  /** 平铺数组 —— 无序，支持 O(1) 尾删 */
  private final Ar<T> array;

  /** 四叉树 —— 用于空间查询，null 表示未启用 */
  private QuadTree<T> tree;

  /** ID → 实体的稀疏数组映射，null 表示未启用 */
  private T[] idMap;

  private int idMapSize = 0;
  private Intf<T> idGetter;

  /** 标记正在批量清理，防止递归 remove */
  private boolean clearing;

  /** 当前迭代索引，用于 remove 时修正 */
  private int iteratorIndex;

  /** 临时矩形复用 */
  private final Rect tmpRect = new Rect();

  /** intersect 结果复用数组 */
  private final Ar<T> intersectResult = new Ar<>(false, 32);

  // ==================== 构造 ====================

  /**
   * @param spatial 是否启用四叉树空间索引
   * @param idMapping 是否启用 ID 映射
   * @param idGetter 从实体提取 ID 的函数（idMapping=true 时必填）
   */
  public EntityAr(boolean spatial, boolean idMapping, Intf<T> idGetter) {
    array = new Ar<>(false, DEFAULT_CAPACITY);

    if (spatial) {
      tree = new QuadTree<>(new Rect(0, 0, 0, 0));
    }

    if (idMapping) {
      if (idGetter == null)
        throw new IllegalArgumentException("idGetter must not be null when idMapping is true");
      this.idGetter = idGetter;
      idMap = (T[]) new QuadTreeObject[DEFAULT_ID_MAP_SIZE];
    }
  }

  /** 无空间索引，无 ID 映射 */
  public EntityAr() {
    this(false, false, null);
  }

  // ==================== 添加 / 移除 ====================

  /** 向组中添加一个实体（同时加入四叉树和 ID 映射） */
  public void add(T entity) {
    if (entity == null) throw new IllegalArgumentException("Cannot add null entity");

    array.add(entity);

    if (tree != null) {
      tree.insert(entity);
    }

    if (idMap != null) {
      putId(entity);
    }
  }

  /** 返回添加后的索引，等同于 {@code add(entity); return array.size - 1;} */
  public int addIndex(T entity) {
    add(entity);
    return array.size - 1;
  }

  /** 从组中移除实体（同时从四叉树和 ID 映射注销） */
  public void remove(T entity) {
    if (clearing || entity == null) return;

    int idx = array.indexOf(entity, true);
    if (idx == -1) return;

    array.remove(idx);

    if (tree != null) {
      tree.remove(entity);
    }

    if (idMap != null) {
      removeId(entity);
    }

    // 修正正在进行的迭代索引
    if (iteratorIndex >= idx) {
      iteratorIndex--;
    }
  }

  /** 按位置快速移除（O(1)），需要调用者保证 position 正确 */
  public void removeIndex(T entity, int position) {
    if (clearing || entity == null) return;
    if (position < 0 || position >= array.size) return;

    // 安全检查：位置可能因并发操作而不正确
    if (array.items[position] != entity) {
      remove(entity); // 回退到慢速实现
      return;
    }

    // O(1) 尾删：把最后一个元素换到当前位置
    if (array.size > 1) {
      T head = array.items[array.size - 1];
      array.items[position] = head;
    }
    array.size--;
    array.items[array.size] = null;

    if (tree != null) {
      tree.remove(entity);
    }

    if (idMap != null) {
      removeId(entity);
    }

    if (iteratorIndex >= position) {
      iteratorIndex--;
    }
  }

  /** 清空所有实体，并对每个实体调用 {@code entityRemoved} 回调 */
  public void clear(Cons<T> entityRemoved) {
    clearing = true;

    for (int i = array.size - 1; i >= 0; i--) {
      entityRemoved.get(array.items[i]);
    }
    array.clear();

    if (tree != null) {
      tree.clear();
    }

    if (idMap != null) {
      for (int i = 0; i < idMapSize; i++) {
        idMap[i] = null;
      }
      idMapSize = 0;
    }

    clearing = false;
  }

  /** 清空所有实体（不调用回调） */
  public void clear() {
    clearing = true;
    array.clear();

    if (tree != null) {
      tree.clear();
    }

    if (idMap != null) {
      for (int i = 0; i < idMapSize; i++) {
        idMap[i] = null;
      }
      idMapSize = 0;
    }
    clearing = false;
  }

  // ==================== 查询 ====================

  public boolean isEmpty() {
    return array.isEmpty();
  }

  public int size() {
    return array.size;
  }

  public T index(int i) {
    return array.get(i);
  }

  /** 返回第一个实体，或 null */
  public T first() {
    return array.firstOpt();
  }

  /** 按谓词查找第一个匹配的实体 */
  public T find(Boolf<T> pred) {
    return array.find(pred);
  }

  /** 是否包含满足谓词的实体 */
  public boolean contains(Boolf<T> pred) {
    return array.contains(pred);
  }

  /** 满足谓词的实体数量 */
  public int count(Boolf<T> pred) {
    return array.count(pred);
  }

  // ==================== ID 映射 ====================

  public boolean mappingEnabled() {
    return idMap != null;
  }

  /** 按 ID 查找实体，O(1)。需要启用 ID 映射。 */
  public T getByID(int id) {
    if (idMap == null) throw new RuntimeException("ID mapping not enabled for this group.");
    if (id < 0 || id >= idMap.length) return null;
    return idMap[id];
  }

  private void putId(T entity) {
    int id = idGetter.get(entity);
    if (id < 0) return; // -1 表示未分配
    if (id >= idMap.length) {
      resizeIdMap(Math.max(id + 1, idMap.length * 2));
    }
    idMap[id] = entity;
    if (id >= idMapSize) idMapSize = id + 1;
  }

  private void removeId(T entity) {
    int id = idGetter.get(entity);
    if (id >= 0 && id < idMap.length) {
      idMap[id] = null;
    }
  }

  private void resizeIdMap(int newSize) {
    T[] newMap = (T[]) new QuadTreeObject[newSize];
    System.arraycopy(idMap, 0, newMap, 0, idMapSize);
    idMap = newMap;
  }

  // ==================== 空间查询 ====================

  public boolean useTree() {
    return tree != null;
  }

  /** 获取四叉树引用（用于手动更新实体在树中的位置） */
  public QuadTree<T> tree() {
    if (tree == null) throw new RuntimeException("Quadtree not enabled for this group.");
    return tree;
  }

  /** 重新设置四叉树的覆盖范围。 当世界尺寸变化时调用。会重建四叉树并重新插入所有实体。 */
  public void resize(float x, float y, float w, float h) {
    if (tree == null) return;

    QuadTree<T> newTree = new QuadTree<>(new Rect(x, y, w, h));
    for (int i = 0; i < array.size; i++) {
      T entity = array.items[i];
      newTree.insert(entity);
    }
    tree = newTree;
  }

  /**
   * 矩形范围查询 —— 无 false positive。 通过回调消费每个命中的实体。
   *
   * <pre>{@code
   * group.intersect(x, y, w, h, entity -> {
   *     // 处理每个命中的 entity
   * });
   * }</pre>
   */
  public void intersect(float x, float y, float w, float h, Cons<T> out) {
    if (isEmpty() || tree == null) return;
    tree.intersect(x, y, w, h, out);
  }

  /**
   * 矩形范围查询，返回命中的第一个实体后终止。
   *
   * @return true 如果找到匹配项
   */
  public boolean intersect(float x, float y, float w, float h, Boolf<T> out) {
    if (isEmpty() || tree == null) return false;
    return tree.intersect(x, y, w, h, out);
  }

  /**
   * 矩形范围查询，返回结果数组（复用内部数组，每次调用前清空）。
   *
   * <pre>{@code
   * Ar<SomeEntity> nearby = group.intersect(cx - r, cy - r, r * 2, r * 2);
   * for (SomeEntity e : nearby) { ... }
   * }</pre>
   */
  public Ar<T> intersect(float x, float y, float w, float h) {
    intersectResult.clear();
    if (isEmpty() || tree == null) return intersectResult;
    tree.intersect(x, y, w, h, intersectResult.toSeq());
    return intersectResult;
  }

  /**
   * 检查范围内是否存在任何实体。
   *
   * @return true 如果范围内至少有一个实体
   */
  public boolean any(float x, float y, float w, float h) {
    if (isEmpty() || tree == null) return false;
    return tree.any(x, y, w, h);
  }

  /** 获取四叉树中所有对象 */
  public void getObjects(Ar<T> out) {
    if (tree != null) {
      tree.getObjects(out.toSeq());
    }
  }

  // ==================== 迭代 ====================

  /** 遍历所有实体 */
  public void each(Cons<? super T> cons) {
    for (int i = 0; i < array.size; i++) {
      cons.get(array.items[i]);
    }
  }

  /** 遍历满足谓词的实体 */
  public void each(Boolf<T> filter, Cons<? super T> cons) {
    for (int i = 0; i < array.size; i++) {
      T item = array.items[i];
      if (filter.get(item)) {
        cons.get(item);
      }
    }
  }

  @Override
  public Iterator<T> iterator() {
    return array.iterator();
  }

  @Override
  public String toString() {
    return "EntityGroup[size="
        + array.size
        + ", spatial="
        + useTree()
        + ", idMapping="
        + mappingEnabled()
        + "]";
  }
}
