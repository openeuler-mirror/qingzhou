package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义了一个名为{@code ModelAction}的注解，用于描述模型层面的操作行为。此注解应用于方法级别，
 * 并在运行时可被获取和解析，提供了对模型操作的各种元数据配置能力。
 *
 * @see ElementType#METHOD 可将此注解应用于方法元素上
 * @see RetentionPolicy#RUNTIME 运行时保留策略，使得注解信息可在运行时通过反射API读取
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

    String condition() default "";

    /**
     * 指定执行该操作后应跳转到的目标页面，默认为空字符串表示无页面转发。
     *
     * @return 转发的目标页面路径
     */
    String forward() default "";

    /**
     * 标识该操作是否支持批量处理，默认为 {@code false}。
     *
     * @return 是否支持批量操作
     */
    boolean batch() default false;

    /**
     * 可用于禁用继承的 action
     */
    boolean disable() default false;
}
