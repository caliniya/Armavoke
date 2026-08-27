package caliniya.vergvoke.base.api;

/**
 * 科技树节点内容：所有可以被加入科技树进行解锁的内容都实现它 （比如单位类型、建筑类型；环境方块这类不需要科技的就不用实现）。
 *
 * <p>数据字段（前置列表等）由 {@code caliniya.vergvoke.base.game.ContentType} 基类预定义，这里只定义行为：读取前置、研究回调、显示名。
 */
public interface TechNodeContent {

  /** 前置内容列表（null/空 = 无前置）。 */
  TechNodeContent[] requirements();

  /** 被研究时触发（解锁内容/效果）。 */
  default void onResearch() {}

  /** 科技树中的显示名，默认用 toString()（ContentType 下即 internalName）。 */
  default String techName() {
    return toString();
  }
}
