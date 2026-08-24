package caliniya.armavoke.annotation.auto;

import caliniya.armavoke.base.anno.auto.*;
import javax.annotation.processing.*;
import javax.tools.Diagnostic;
import caliniya.armavoke.annotation.Processor;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import java.util.Set;

@AnnoProc
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("caliniya.armavoke.annotation.Annotations.SystemDef")
public class TestAnno extends Processor {

  @Override
  public void process() {
    messager.printMessage(Diagnostic.Kind.NOTE, "TTTT");
  }
}
