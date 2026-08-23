package caliniya.armavoke.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface SystemDef {
  String name();

  String thread();

  int priority() default 0;

  Class<?>[] reads() default {};

  Class<?>[] writes() default {};

  boolean parallel() default false;

  int interval() default 1;

  String[] after() default {};
}
