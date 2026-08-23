package caliniya.armavoke.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface FieldOpt {
  boolean volatileField() default false;

  boolean readonly() default false;

  String defaultValue() default "";

  boolean persist() default true;
}
