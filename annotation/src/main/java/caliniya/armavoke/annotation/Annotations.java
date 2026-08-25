package caliniya.armavoke.annotation;

import java.lang.annotation.*;

public class Annotations {
  
  // 声明一个组件
  @Target({ElementType.TYPE})
  public @interface Component{
    String name();
  }
  
  //这说明这个字段是从实体中其他组件读起来的，不应该将它注入到实体中以避免覆盖
  // 如果没有找到这个字段 那么就应该报错
  @Target({ElementType.FIELD})
  public @interface Import{
    
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