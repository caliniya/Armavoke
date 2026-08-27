package caliniya.vergvoke.base.anno.auto;

import java.lang.annotation.*;

// 标记这个类是一个注解处理器
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface AnnoProc {}
