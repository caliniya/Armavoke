package caliniya.armavoke.map;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Texture;
import arc.struct.StringMap;
import arc.util.Strings;

/** 代表一个游戏地图或存档的元数据对象。 不包含实际的游戏网格数据，只包含描述信息。 */
public class Map implements Comparable<Map> {

  /** 地图文件引用 */
  public final Fi file;

  /** 是否是内置地图 */
  public final boolean custom;

  /** 元数据标签 (名称, 作者, 描述, 版本等) */
  public final StringMap tags;

  /** 地图尺寸 (从文件头读取) */
  public int width, height;
  
  /** 是否是太空地图 */
  public final boolean space;

  /** 预览图 */
  public Texture texture;

  // 构造函数
  public Map(Fi file, int width, int height, StringMap tags, boolean custom) {
    this.file = file;
    this.width = width;
    this.height = height;
    this.tags = tags;
    this.custom = custom;
    this.space = tags.getBool("space");
  }

  public String name() {
    return tags.get("name", file.nameWithoutExtension());
  }

  public String author() {
    return tags.get("author", "Unknown");
  }

  public String description() {
    return tags.get("description", "");
  }

  public String plainName() {
    return Strings.stripColors(name());
  }

  /** 获取预览图，如果没有则返回默认错误图 */
  public Texture safeTexture() {
    if (texture == null) {
      //return Core.assets.get("sprites/error.png", Texture.class);
      return null;
    }
    return texture;
  }

  @Override
  public String toString() {
    return "GameMap{" + file.name() + "}";
  }

  @Override
  public int compareTo(Map other) {
    // 先按是否自定义排序，再按名称排序
    int type = Boolean.compare(this.custom, other.custom);
    if (type != 0) return type;
    return this.plainName().compareTo(other.plainName());
  }
}