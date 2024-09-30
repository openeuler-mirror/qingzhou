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
    String code(); // 接口名

    String icon() default ""; // 指定与该模型操作关联的图标名称

    boolean list() default false; // 是否显示在列表中。

    boolean head() default false; // 是否显示到列表头部

    int order() default 0; // 设置该操作在列表页面上的显示次序，负数则表示不在页面上显示

    boolean batch() default false; // 该操作是否支持批量处理

    String page() default ""; // 由指定的视图处理

    String show() default ""; // 设置该操作的可用条件

    boolean disable() default false; // 是否禁用此 action

    /**
     * 提供一个国际化资源键数组，用于根据不同的语言环境加载相应的操作名称。
     */
    String[] name();

    /**
     * 提供一个国际化资源键数组，用于根据不同的语言环境加载相应的操作详细信息。
     */
    String[] info();

    String[] showFields() default {};

    boolean showToListHead() default false;

}
