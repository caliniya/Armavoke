package caliniya.armavoke.core;

import arc.struct.ObjectMap;
import caliniya.armavoke.base.game.ContentType;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.CType;

public class ContentVar {
  
  // 全局映射表: "Unit.xxx" -> Object
  private static final ObjectMap<String, ContentType> contentMap = new ObjectMap<>();

  // 分类列表，用于通过 ID 查找对象
  // contentByTypes[CType.ordinal()].get(id - 1)
  private static final Ar<ContentType>[] contentByTypes;

  static {
    int typeCount = CType.values().length;
    contentByTypes = new Ar[typeCount];
    for (int i = 0; i < typeCount; i++) {
      contentByTypes[i] = new Ar<>();
    }
  }
  
  /** 注册内容并分配 ID */
  public static void add(ContentType content) {
    if (content == null) return;
    
    // 存入名字映射
    contentMap.put(content.internalName, content);
    
    Ar<ContentType> list = contentByTypes[content.type.ordinal()];
    
    if (list.size >= Short.MAX_VALUE) {
        throw new RuntimeException("Too many contents for type: " + content.type + ". Max is " + Short.MAX_VALUE);
    }
    
    content.id = (short) (list.size + 1);
    
    list.add(content);
  }

  public static ContentType get(String internalName) {
    return contentMap.get(internalName);
  }

  @SuppressWarnings("unchecked")
  public static <T extends ContentType> T get(String internalName, Class<T> type) {
    ContentType c = contentMap.get(internalName);
    if (type.isInstance(c)) {
      return (T) c;
    }
    return null;
  }

  public static Ar<ContentType> getByType(CType type) {
    return contentByTypes[type.ordinal()];
  }
  
  /** 
   * 通过 ID 和 类型 获取对象 
   * @param type 内容类型 (Floor, Unit, etc.)
   * @param id 运行时 ID
   * @return 对应的对象，如果 ID 为 0 或越界则返回 null
   */
  @SuppressWarnings("unchecked")
  public static <T extends ContentType> T getByID(CType type, short id) {
      if (id <= 0) return null; // 0 是保留空值
      
      Ar<ContentType> list = contentByTypes[type.ordinal()];
      
      int index = id - 1;
      
      if (index >= list.size) return null; // 防止越界
      
      return (T) list.get(index);
  }

  public static void clear() {
    contentMap.clear();
    for (Ar<ContentType> list : contentByTypes) {
      list.clear();
    }
  }
}