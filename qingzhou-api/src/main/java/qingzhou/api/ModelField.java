package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>该注解用于标注模型字段，应用于字段级别，并在运行时提供对字段的检索和处理能力。</p>
 *
 * <p>注意：此注解不支持原数据查询，如唯一性校验等需要比对源库数据的操作。
 * 这类逻辑不应在此注解中实现，而应在具体的业务逻辑Action中处理。</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelField {
    /**
     * 获取字段的国际化名称数组，用于多语言环境下展示在用户界面。
     *
     * @return 字段国际化名称数组
     */
    String[] name();

    /**
     * 获取字段的国际化描述信息数组，用于提供字段的详细说明信息。
     *
     * @return 字段国际化描述信息数组
     */
    String[] info();

    /**
     * 设置字段所属的页面表单展示分组，默认为空字符串表示“其它”分组。
     */
    String group() default "";

    /**
     * 字段的显示类型
     */
    FieldType type() default FieldType.text;

    /**
     * 字段的取值范围
     */
    String[] options() default "";

    // 若 refModel 有，则 options() 会列出指定 model 的所有 id
    Class<? extends ModelBase> refModel() default ModelBase.class;

    /**
     * 定义可见性条件的函数。
     * 该函数用于指定一个条件，根据该条件确定某个元素是否可见。
     * 默认情况下，如果没有指定条件，则元素总是可见的。
     *
     * @return 返回一个字符串，表示可见性的条件。如果条件为空字符串，则表示元素总是可见的。
     */
    String show() default "";

    /**
     * 是否显示在列表中。
     */
    boolean list() default false;

    /**
     * 仅用于数据监视
     */
    boolean monitor() default false;

    /**
     * 用于数据监视时，其值是否是数字类型的，数字类型的值可用于绘制折线图、统计分析等
     */
    boolean numeric() default false;

    /*********** 校验信息 ***********/
    boolean createable() default true; // 允许创建时指定值

    boolean editable() default true; // 允许编辑此字段

    boolean required() default true;

    long min() default Long.MIN_VALUE;

    long max() default Long.MAX_VALUE;

    int lengthMin() default -1;

    int lengthMax() default Integer.MAX_VALUE;

    boolean port() default false;

    String unsupportedCharacters() default "";

    String[] unsupportedStrings() default {};

    String pattern() default "";

    boolean email() default false;
}
