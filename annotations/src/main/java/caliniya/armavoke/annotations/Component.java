package caliniya.armavoke.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Component {
  String name();

  String updateBy() default "";

  Class<?>[] requires() default {};

  boolean pure() default true;

  boolean pooled() default false;

  Storage storage() default Storage.Inline;
}
