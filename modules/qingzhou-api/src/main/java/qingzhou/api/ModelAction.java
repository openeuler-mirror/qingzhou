package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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

    String[] info() default {};

    // 确认提示信息，国际化数组。非空时前端在执行操作前弹出确认对话框。
    String[] confirm() default {};
}
