package caliniya.armavoke.annotation;

import java.lang.annotation.*;

public class Annotations {
  
  // 声明一个组件
  @Target({ElementType.TYPE})
  public @interface Component{
    String name();
  }
  
  // 声明一种实体
  @Target({ElementType.TYPE})
  public @interface Entity{
    String name();
  }
  
  // 声明一个系统
  @Target({ElementType.TYPE})
  public @interface SystemDef{
    String name();
    String thread();
    int index();
  }
  
  // 声明一个线程
  @Target({ElementType.TYPE})
  public @interface ThreadDef{
    String name();
  }
  
  // 标记组件的更新方法
  @Target({ElementType.METHOD})
  public @interface Updata{
  }
  
}