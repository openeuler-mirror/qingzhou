package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义模型的操作。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface ModelAction {
    String code() default "";

    String icon() default "";

    int order() default 0;

    String display() default ""; // 设置该操作的可用条件

    boolean add() default false;

    boolean update() default false;

    boolean show() default false;

    boolean list() default false;

    boolean batch() default false;

    String[] name() default {};

    // 提供一个国际化资源键数组，用于根据不同的语言环境加载相应的操作详细信息。
    String[] info() default {};
}
