package caliniya.armavoke.annotation;

import java.lang.annotation.*;

public class Annotations {
  
  @Target({ElementType.TYPE})
  public @interface Component{
    
  }
  
  @Target({ElementType.TYPE})
  public @interface SystemDef{
    String name();
    String thread();
  }
  
  @Target({ElementType.TYPE})
  public @interface ThreadDef{
    String name();
    
  }
  
  @Target({ElementType.METHOD})
  public @interface Updata{
    
  }
  
}