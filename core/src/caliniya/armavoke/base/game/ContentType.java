package caliniya.armavoke.base.game;

import arc.Core;
import arc.util.Nullable;
import caliniya.armavoke.base.type.*;
import caliniya.armavoke.core.*;

public class ContentType {

  public final String name;
  public final CType type;
  
  public final String internalName; 

  //用于UI显示的本地化名称
  public String localizedName;
  public @Nullable String description;

  public ContentType(String name, CType type) {
    this.name = name;
    this.type = type;
    
    this.internalName = type.name() + "." + name;
    
    this.localizedName = Core.bundle.get(internalName + ".name", name);
    this.description = Core.bundle.getOrNull(internalName + ".description");

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