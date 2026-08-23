package caliniya.armavoke.world.stars;

/** 宇宙中所有共享数据 */
public class Universe {

    /** 当前点击选中的星系节点。 */
    public static StarNode selectedNode;

    /** 鼠标当前悬停的星系节点。 */
    public static StarNode hoverNode;

    public static void clearSelection() {
        selectedNode = null;
        hoverNode = null;
    }
}
