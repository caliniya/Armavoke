package caliniya.armavoke.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Repeatable(Imports.class)
public @interface Import {
  Class<?> component();

  String[] fields() default {};

  AccessMode mode() default AccessMode.ReadOnly;
}
