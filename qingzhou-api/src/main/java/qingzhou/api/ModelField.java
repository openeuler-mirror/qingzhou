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

    FieldType type() default FieldType.text;

    String[] options() default "";

    // 若 refModel 有，则 options() 会列出指定 model 的所有 id
    String refModel() default "";

    /**
     * 是否显示在列表中。
     */
    boolean list() default false;

    boolean monitor() default false;

    boolean numeric() default false;

    boolean required() default true;

    long min() default Long.MIN_VALUE;

    long max() default Long.MAX_VALUE;

    int lengthMin() default -1;

    int lengthMax() default Integer.MAX_VALUE;

    boolean port() default false;

    String unsupportedCharacters() default "";

    String[] unsupportedStrings() default {};
}
