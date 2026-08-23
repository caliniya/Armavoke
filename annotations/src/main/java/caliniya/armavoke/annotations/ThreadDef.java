package caliniya.armavoke.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ThreadDef {
  String name();

  int workers() default 1;

  int priority() default Thread.NORM_PRIORITY;

  boolean interruptible() default true;
}
