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
     * @return 操作的名称
     */
    String name();

    /**
     * 指定与该模型操作关联的图标名称。
     * @return 图标名称
     * @defaultValue ""
     */
    String icon() default "";

    /**
     * 提供一个国际化资源键数组，用于根据不同的语言环境加载相应的操作名称。
     * @return 国际化资源键数组
     */
    String[] nameI18n();

    /**
     * 提供一个国际化资源键数组，用于根据不同的语言环境加载相应的操作详细信息。
     * @return 国际化资源键数组
     */
    String[] infoI18n();

    /**
     * 设置操作生效的条件表达式，默认为空字符串，表示无特定生效条件。
     * @return 操作生效的条件表达式
     * @defaultValue ""
     */
    String effectiveWhen() default "";

    /**
     * 指定执行该操作后应跳转到的目标页面，默认为空字符串表示无页面转发。
     * @return 转发的目标页面路径
     * @defaultValue ""
     */
    String forwardToPage() default "";

    /**
     * 标识是否应在列表头部显示此操作，默认为 {@code false}。
     * @return 是否显示在列表头部
     * @defaultValue false
     */
    boolean showToListHead() default false;

    /**
     * 标识是否应在列表项中显示此操作，默认为 {@code false}。
     * @return 是否显示在列表中
     * @defaultValue false
     */
    boolean showToList() default false;

    /**
     * 在列表中展示操作时的排序顺序，默认为0，数值越小排序越靠前。
     * @return 列表中的显示顺序
     * @defaultValue 0
     */
    int orderOnList() default 0;

    /**
     * 标识该操作是否支持批量处理，默认为 {@code false}。
     * @return 是否支持批量操作
     * @defaultValue false
     */
    boolean supportBatch() default false;

    /**
     * 标识该操作是否默认禁用（不可用），默认为 {@code false}。
     * @return 是否禁用该操作
     * @defaultValue false
     */
    boolean disabled() default false;
}

