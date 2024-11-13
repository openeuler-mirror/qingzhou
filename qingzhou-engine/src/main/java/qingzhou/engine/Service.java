package qingzhou.engine;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Service {
    // 默认为服务现类的 class simple name
    String name() default "";

    // 表示此服务是否可供应用共享使用
    boolean shareable() default true;

    // 默认为服务现类的 class name
    String description() default "";
}
