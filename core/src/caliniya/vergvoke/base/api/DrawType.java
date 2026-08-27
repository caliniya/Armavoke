package caliniya.vergvoke.base.api;

//为所有具有绘制能力的模板提供一个统一接口(虽然我目前还没想到用处)
public interface DrawType<T> {
	public void draw(T t);
  public void drawDebug(T t);
}