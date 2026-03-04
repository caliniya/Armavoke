package caliniya.armavoke.type.module;

import arc.util.io.Reads;
import arc.util.io.Writes;
import caliniya.armavoke.core.ContentVar;
import caliniya.armavoke.type.Item;
import caliniya.armavoke.type.type.ItemType;

/**
 * 物品存储模块，用于管理不同类型物品的存储、容量限制及过滤规则。
 *
 * <p>该模块继承自 {@link Module}，使用数组结构存储物品数量，支持序列化与反序列化。 物品的存储基于物品ID进行索引，支持设置每种物品的统一容量上限。
 @author caliniya
 @version 1.0
 */
public class ItemModule extends Module {

  /** 每种物品的存储上限 */
  public int capacity = 100;

  /**
   * 存储数组。
   *
   * <p>索引 = 物品ID (type.id)<br>
   * 值 = 当前数量<br>
   * 大小 = 最大ID + 1 (因为 ID 从 1 开始，0 索引废弃)
   */
  protected int[] items;

  /**
   * 过滤数组。
   *
   * <p>索引 = 物品ID<br>
   * 值 = true (允许存储), false (不允许)<br>
   * 如果为 null，则允许所有物品。
   */
  protected boolean[] filter;

  /**
   * 构造一个具有指定容量的物品模块。
   *
   * <p>初始化内部存储数组，大小基于当前加载的物品总数量 ({@link ContentVar#totalItemCount})。 这确保了所有已注册的物品都有对应的存储位置。
   *
   * @param capacity 每种物品的存储上限
   */
  public ItemModule(int capacity) {
    this.capacity = capacity;
    // 初始化数组大小。ID 是从 1 开始递增的，所以数组长度需要 +1
    // 使用 totalItemCount + 1 也是安全的，只要ID是连续分配的
    int size = ContentVar.totalItemCount + 1;

    // 防止 totalItemCount 为 0 时数组过小（通常启动时至少加载了核心物品）
    if (size < 10) size = 10;

    this.items = new int[size];
  }

  /**
   * 设置过滤器：仅允许指定的物品类型存入。
   *
   * <p>调用此方法将初始化内部过滤数组，默认情况下所有物品将被拒绝， 只有传入的物品类型会被标记为允许。
   *
   * @param types 允许的物品列表，如果为 null 或空则视作无过滤（取决于实现，此处会重置数组）
   */
  public void setFilter(ItemType... types) {
    // 初始化 filter 数组，Java boolean 数组默认全为 false
    filter = new boolean[items.length];

    if (types != null) {
      for (ItemType type : types) {
        if (type != null && type.id < filter.length) {
          filter[type.id] = true;
        }
      }
    }
  }

  /**
   * 清除过滤器，允许所有物品存入。
   *
   * <p>将内部过滤数组置为 null，移除所有限制。
   */
  public void clearFilter() {
    filter = null;
  }

  /**
   * 检查该模块是否允许存储指定类型的物品。
   *
   * <p>检查逻辑包括：物品是否为 null、ID是否越界、以及是否通过过滤器检查。
   *
   * @param type 要检查的物品类型
   * @return 如果允许存储返回 {@code true}，否则返回 {@code false}
   */
  public boolean accepts(ItemType type) {
    if (type == null) return false;
    // 边界检查
    if (type.id >= items.length) return false;

    // 如果 filter 为 null，默认允许；否则检查对应索引
    return filter == null || filter[type.id];
  }

  /**
   * 尝试向模块中添加指定数量的某种物品。
   *
   * <p>实际添加的数量受限于剩余容量。
   *
   * @param type 物品类型
   * @param amount 想要添加的数量
   * @return 实际添加的数量（如果无法添加或物品被过滤，返回 0）
   */
  public int addItem(ItemType type, int amount) {
    if (!accepts(type) || amount <= 0) return 0;

    // 数组直接访问，O(1) 复杂度
    int current = items[type.id];
    int space = capacity - current;

    if (space <= 0) return 0;

    int added = Math.min(amount, space);
    items[type.id] = current + added;

    return added;
  }

  /**
   * 尝试向模块中添加一个物品对象。
   *
   * <p>会检查物品是否为空。
   *
   * @param item 包含类型和数量的物品对象
   * @return 实际添加的数量
   */
  public int addItem(Item item) {
    if (item == null || item.isEmpty()) return 0;
    return addItem(item.type, item.amount);
  }

  /**
   * 尝试从模块中移除指定数量的某种物品。
   *
   * <p>实际移除的数量受限于当前库存量。
   *
   * @param type 物品类型
   * @param amount 想要移除的数量
   * @return 实际移除的数量（如果库存不足或物品无效，返回相应可移除数量或 0）
   */
  public int removeItem(ItemType type, int amount) {
    if (type == null || amount <= 0) return 0;
    if (type.id >= items.length) return 0;

    int current = items[type.id];
    if (current <= 0) return 0;

    int removed = Math.min(amount, current);
    items[type.id] = current - removed;

    return removed;
  }

  /**
   * 获取指定类型物品的当前存储数量。
   *
   * @param type 物品类型
   * @return 当前数量，如果类型无效则返回 0
   */
  public int getAmount(ItemType type) {
    if (type == null || type.id >= items.length) return 0;
    return items[type.id];
  }

  // --- 存档逻辑 ---
  // 为了保持存档兼容性，保存时依然遍历有效的物品ID

  /**
   * 将模块数据写入输出流。
   *
   * <p>写入内容包括：容量、过滤器配置、以及当前存储的物品数据。 仅写入数量大于 0 的物品以节省空间。
   *
   * @param write 写入流
   */
  @Override
  public void write(Writes write) {
    write.i(capacity);

    // 写入过滤器状态
    if (filter != null) {
      write.bool(true);
      int count = 0;
      // 先统计有多少个 true，或者直接写整个数组的标志位
      // 为了节省空间，只写 true 的 ID 列表
      for (int i = 1; i < filter.length; i++) {
        if (filter[i]) count++;
      }
      write.s((short) count);
      for (int i = 1; i < filter.length; i++) {
        if (filter[i]) write.s((short) i);
      }
    } else {
      write.bool(false);
    }

    // 写入物品数据：只写入数量 > 0 的物品
    int count = 0;
    // 预先遍历一次计数（或者假设足够小直接写，但计数更安全）
    for (int i = 1; i < items.length; i++) {
      if (items[i] > 0) count++;
    }
    write.s((short) count);

    for (int i = 1; i < items.length; i++) {
      int amt = items[i];
      if (amt > 0) {
        write.s((short) i); // 写入 ID (索引)
        write.i(amt); // 写入数量
      }
    }
  }

  /**
   * 从输入流读取模块数据。
   *
   * <p>读取内容并覆盖当前的容量、过滤器配置及物品存储状态。 读取前会清空现有数据。
   *
   * @param read 读取流
   */
  @Override
  public void read(Reads read) {
    this.capacity = read.i();

    // 读取过滤器
    if (read.bool()) {
      short filterCount = read.s();
      // 确保 filter 数组已初始化且大小足够
      if (filter == null || filter.length != items.length) {
        filter = new boolean[items.length];
      } else {
        // 清空旧数据
        for (int i = 0; i < filter.length; i++) filter[i] = false;
      }

      for (int i = 0; i < filterCount; i++) {
        short id = read.s();
        if (id < filter.length) {
          filter[id] = true;
        }
      }
    } else {
      filter = null;
    }

    // 读取物品
    short itemCount = read.s();
    // 清空现有数据
    for (int i = 0; i < items.length; i++) items[i] = 0;

    for (int i = 0; i < itemCount; i++) {
      short id = read.s();
      int amt = read.i();
      if (id < items.length) {
        items[id] = amt;
      }
    }
  }
}
