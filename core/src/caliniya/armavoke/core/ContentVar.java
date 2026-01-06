package caliniya.armavoke.core;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import caliniya.armavoke.base.game.ContentType;
import caliniya.armavoke.base.tool.Ar;
import caliniya.armavoke.base.type.CType;

public class ContentVar {
  
  // 全局映射表: "Unit.dagger" -> Object
  private static final ObjectMap<String, ContentType> contentMap = new ObjectMap<>();

  private static final Ar<ContentType>[] contentByTypes;

  static {
    // 初始化分类数组
    int typeCount = CType.values().length;
    contentByTypes = new Ar[typeCount];
    for (int i = 0; i < typeCount; i++) {
      contentByTypes[i] = new Ar<>();
    }
  }
  
  /** 注册内容 */
  public static void add(ContentType content) {
    if (content == null) return;
    
    contentMap.put(content.internalName, content);
    
    contentByTypes[content.type.ordinal()].add(content);
  }

  public static ContentType get(String internalName) {
    return contentMap.get(internalName);
  }

  /** 带类型的查找 (强制转换) */
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
  
  /** 清理数据 (用于重载或重置游戏时) */
  public static void clear() {
    contentMap.clear();
    for (Ar<ContentType> list : contentByTypes) {
      list.clear();
    }
  }
}