package caliniya.armavoke.annotation.auto;

import caliniya.armavoke.base.anno.auto.*;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import java.util.Set;

@AnnoProc
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("*")
public class TestAnno extends AbstractProcessor {
  
  @Override
public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    return false;
}
}
