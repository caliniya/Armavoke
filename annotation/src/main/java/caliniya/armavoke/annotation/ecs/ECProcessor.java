package caliniya.armavoke.annotation.ecs;

import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import caliniya.armavoke.annotation.tool.AElement;
import caliniya.armavoke.annotation.tool.AType;
import caliniya.armavoke.annotation.tool.AVar;
import caliniya.armavoke.base.tool.Ar;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.util.*;
import caliniya.armavoke.annotation.Processor;
import caliniya.armavoke.annotation.Annotations.*;
import caliniya.armavoke.base.anno.auto.AnnoProc;

@AnnoProc
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({
  "caliniya.armavoke.annotation.Annotations.Component",
  "caliniya.armavoke.annotation.Annotations.Entity"
})
public class ECProcessor extends Processor {

  public ObjectMap<String, ObjectSet<String>> nameMap = new ObjectMap<>();
  public Ar<AType> entityDef = new Ar<>();
  
  {
    
  }

  @Override
  protected void process() {

    entityDef.addAll(types(Entity.class));

    for (AType a : entityDef) {
      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
          a.annotation(Entity.class).getElementValues().entrySet()) {
        String key = entry.getKey().getSimpleName().toString();
        Object value = entry.getValue().getValue();
        if (key.equals("name")) {
          nameMap.put((String) value, new ObjectSet<>());
        }
      }
    }

    for (AType T : types(Component.class)) {
      for (AVar V : T.fields()) {
        /**
         * if(nameMap.co) {
         *
         * <p>}
         */
      }
    }
  }
}
