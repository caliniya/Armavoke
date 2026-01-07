package caliniya.armavoke.base.game;

import arc.Core;
import arc.util.Nullable;
import caliniya.armavoke.base.type.*;
import caliniya.armavoke.core.*;

public class ContentType {

  public final String name;
  public final CType type;
  
  public final String internalName; 
  
  // 范围 1 ~ 32767 (0 保留为空)
  public short id; 

  public String localizedName;
  public @Nullable String description;

  public ContentType(String name, CType type) {
    this.name = name;
    this.type = type;
    
    this.internalName = type.name() + "." + name;
    
    this.localizedName = Core.bundle.get(internalName + ".name", name);
    this.description = Core.bundle.getOrNull(internalName + ".description");

    // 注册时会自动分配 ID
    ContentVar.add(this);
  }
  
  public String getIdentity() {
      return internalName;
  }

  @Override
  public String toString() {
    return internalName;
  }
}