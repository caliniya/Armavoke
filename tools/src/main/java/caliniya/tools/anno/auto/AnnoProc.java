package caliniya.armavoke.tools.anno.auto;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE) // 仅编译时需要
@Target(ElementType.TYPE) // 标记在类上
public @interface AnnoProc {}
