package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义模型的操作。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelAction {
    String icon() default ""; // 指定与该模型操作关联的图标名称

    String show() default ""; // 设置该操作的可用条件

    int order() default 0; // 设置该操作在列表页面上的显示次序，负数则表示不在页面上显示

    boolean ajax() default false;

    boolean batch() default false; // 标识该操作是否支持批量处理

    boolean disable() default false; // 是否禁用此 action

    /**
     * 提供一个国际化资源键数组，用于根据不同的语言环境加载相应的操作名称。
     */
    String[] name();

    /**
     * 提供一个国际化资源键数组，用于根据不同的语言环境加载相应的操作详细信息。
     */
    String[] info();
}
