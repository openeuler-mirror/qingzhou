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
     * 指定模型操作的名称，通常是一个简短易懂的字符串标识符。
     *
     * @return 操作的名称
     */
    String name();

    /**
     * 提供一个国际化资源键数组，用于根据不同的语言环境加载相应的操作名称。
     *
     * @return 国际化资源键数组
     */
    String[] nameI18n();

    /**
     * 提供一个国际化资源键数组，用于根据不同的语言环境加载相应的操作详细信息。
     *
     * @return 国际化资源键数组
     */
    String[] infoI18n();

    /**
     * 设置操作生效的条件表达式，默认为空字符串，表示无特定生效条件。
     *
     * @return 操作生效的条件表达式
     */
    String effectiveWhen() default "";

    /**
     * 标识该操作是否支持批量处理，默认为 {@code false}。
     *
     * @return 是否支持批量操作
     */
    boolean supportBatch() default false;

    /**
     * 可用于禁用继承的 action
     */
    boolean disabled() default false;
}

