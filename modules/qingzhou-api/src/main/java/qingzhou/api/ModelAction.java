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

    int order() default 9;

    String display() default ""; // 设置该操作的可用条件

    boolean add() default false;

    boolean update() default false;

    boolean show() default false;

    boolean list_head() default false; // 列表页面的头部显示

    boolean list() default false; // 列表页面每条数据后面显示

    boolean batch() default false;

    String[] name() default {};

    // 提供一个国际化资源键数组，用于根据不同的语言环境加载相应的操作详细信息。
    String[] info() default {};
}
