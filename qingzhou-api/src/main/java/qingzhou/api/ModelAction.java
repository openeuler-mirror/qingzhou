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
    /**
     * 提供一个国际化资源键数组，用于根据不同的语言环境加载相应的操作名称。
     *
     * @return 国际化资源键数组
     */
    String[] name();

    /**
     * 提供一个国际化资源键数组，用于根据不同的语言环境加载相应的操作详细信息。
     *
     * @return 国际化资源键数组
     */
    String[] info();

    /**
     * 指定与该模型操作关联的图标名称。
     *
     * @return 图标名称
     */
    String icon() default "";

    int order() default 0;

    String show() default "";

    /**
     * 标识该操作是否支持批量处理
     */
    boolean batch() default false;

    /**
     * 可用于禁用继承的 action
     */
    boolean disable() default false;
}
