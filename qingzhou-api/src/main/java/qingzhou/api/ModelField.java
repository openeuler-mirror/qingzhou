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
    String group() default ""; // 设置字段所属的页面表单展示分组，默认为空字符串表示“其它”分组。

    FieldType type() default FieldType.text; // 字段的显示类型

    String[] options() default ""; // 字段的取值范围

    Class<? extends ModelBase> refModel() default ModelBase.class; // 若 refModel 有，则 options() 会列出指定 model 的所有 id

    boolean required() default true;

    boolean createable() default true; // 允许创建时指定值

    boolean editable() default true; // 允许编辑此字段

    String show() default ""; // 标识此字段有效的条件

    long min() default Long.MIN_VALUE;

    long max() default Long.MAX_VALUE;

    int lengthMin() default -1;

    int lengthMax() default Integer.MAX_VALUE;

    boolean port() default false;

    String unsupportedCharacters() default "";

    String[] unsupportedStrings() default {};

    String pattern() default "";

    boolean email() default false;

    boolean monitor() default false; // 标识该属性为监视类型，而非表单项

    boolean numeric() default false;  // 标识该属性为监视类型中的动态数字类型，可用于绘制折线图。在该属性为监视类型时有效。

    boolean list() default false; // 是否显示在列表中。

    /**
     * 获取字段的国际化名称数组，用于多语言环境下展示在用户界面。
     */
    String[] name();

    /**
     * 获取字段的国际化描述信息数组，用于提供字段的详细说明信息。
     */
    String[] info();
}
