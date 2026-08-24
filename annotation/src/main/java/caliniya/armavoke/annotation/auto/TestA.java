package caliniya.armavoke.annotation.auto;

import caliniya.armavoke.annotation.Processor;
import javax.tools.Diagnostic;
import javax.lang.model.SourceVersion;
import javax.annotation.processing.*;
import caliniya.armavoke.base.anno.auto.AnnoProc;

@AnnoProc
@SupportedOptions({"org.gradle.annotation.processing.isolating"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("caliniya.armavoke.annotation.Annotations.Entity")
public class TestA extends Processor {

  @Override
  protected void process() {
    messager.printMessage(Diagnostic.Kind.NOTE, "TestAA");
  }
}
