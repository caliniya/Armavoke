package caliniya.vergvoke.annotation.ecs;

import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import caliniya.vergvoke.annotation.Annotations.*;
import caliniya.vergvoke.annotation.Processor;
import caliniya.vergvoke.annotation.tool.AElement;
import caliniya.vergvoke.annotation.tool.AType;
import caliniya.vergvoke.annotation.tool.AVar;
import caliniya.vergvoke.base.anno.auto.AnnoProc;
import caliniya.vergvoke.base.tool.Ar;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;

import javax.tools.StandardLocation;

@AnnoProc
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes({
  "caliniya.vergvoke.annotation.Annotations.Component",
  "caliniya.vergvoke.annotation.Annotations.Entity"
})
public class ECProcessor extends Processor {

  // 每种类型的实体都有哪些组件
  public ObjectMap<String, ObjectSet<String>> ECMap = new ObjectMap<>();
  public Ar<AType> entityDef = new Ar<>();

  {
  }

  @Override
  protected void process() {

    entityDef.addAll(types(Entity.class));

    for (AType a : entityDef) {
      ObjectSet<String> map = new ObjectSet<String>();
      for (Class<?> c : a.annotation(Entity.class).comps()) {
        if (map.contains(c.getSimpleName())) {
          error(c.getSimpleName() + "Already exist");
        }
        map.add(c.getSimpleName());
      }
      ECMap.put(a.annotation(Entity.class).name() + "Entity", map);
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
