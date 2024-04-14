package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ActionView {
    /**
     * 指定与该模型操作关联的图标名称。
     *
     * @return 图标名称
     */
    String icon() default "";


    /**
     * 指定执行该操作后应跳转到的目标页面，默认为空字符串表示无页面转发。
     *
     * @return 转发的目标页面路径
     */
    String forwardTo() default "";


    /**
     * 显示在列表中的次序，小于 1 则不显示
     */
    int shownOnList() default 0;

    /**
     * 标识是否应在列表头部显示此操作，默认为 {@code false}。
     *
     * @return 是否显示在列表头部
     */
    int shownOnListHead() default 0;
}
