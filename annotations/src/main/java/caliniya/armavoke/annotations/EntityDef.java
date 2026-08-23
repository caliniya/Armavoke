package caliniya.armavoke.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface EntityDef {
  String name();

  Class<?>[] components();

  Class<?>[] abilities() default {};

  Class<?>[] modules() default {};

  boolean pooled() default true;

  boolean serializable() default true;

  String generatedClass() default "";

  String constructor() default "";
}
