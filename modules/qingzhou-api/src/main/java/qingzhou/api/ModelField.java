package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注模型字段，运行时提供对字段的检索和处理能力。
 * 注意：此注解不支持原数据查询，如唯一性校验等需要比对源库数据的操作，
 * 这类逻辑应在具体的业务逻辑 Action 中处理。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface ModelField {
    String code() default "";

    FieldType field_type() default FieldType.form;

    InputType input_type() default InputType.text;

    String display() default "";

    String ref_model() default ""; // 使用指定的模块的所有数据id作为字段的取值范围

    String link_to() default ""; // 点击该字段值时跳转到指定模型的列表页并筛选，格式为 modelCode.fieldCode

    String[] options() default {};

    String separator() default ","; // 用以分割多值

    boolean id() default false; // 当 Model 为 Page 类型时，用此字段作 ID

    boolean readonly() default false; // 在form页面显示为无样式 会传值 样式为readonly

    boolean required() default false;

    long min_value() default Long.MIN_VALUE;

    long max_value() default Long.MAX_VALUE;

    int min_length() default -1;

    int max_length() default Integer.MAX_VALUE;

    boolean host() default false;

    boolean port() default false;

    boolean email() default false;

    boolean file() default false;

    String pattern() default "";

    // 列表类型的相关信息

    boolean add() default true;

    boolean update() default true;

    boolean show() default true;

    boolean list() default false;

    boolean search() default false;

    boolean numeric() default false; // 监视类型中的动态数字，可用于绘制折线图

    ChartType chart_type() default ChartType.line; // 监视字段的图表类型，仅在 numeric=true 时有效

    String group() default ""; // 字段所属一个表单的分组，引用 @I18n 的 code 值。

    String chart_title() default ""; // 字段所属图表的标题，引用 @I18n 的 code 值。

    String[] color() default {}; // 用于样式转换，形式：{"当前字段值:#f7f7f7", "当前字段值:#xxxxxx"}

    int width_percent() default -1;

    String[] name();

    String[] info() default {};
}
