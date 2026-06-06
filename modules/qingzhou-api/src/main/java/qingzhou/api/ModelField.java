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
@Retention(RetentionPolicy.CLASS)
public @interface ModelField {
    String code() default "";

    FieldType field_type() default FieldType.FORM;

    InputType input_type() default InputType.text; // 字段的显示类型

    String display() default ""; // 在页面上显示或隐藏的条件

    String ref_model() default ""; // 使用指定的模块的所有数据id作为字段的取值范围

    String link_to() default ""; // 点击该字段值时跳转到指定模型的列表页并筛选，格式为 modelCode.fieldCode

    String[] options() default {};

    String separator() default ","; // 当使用 options() 或 refModel()，用以分割多值

    boolean id() default false; // 当 Model 为 List 类型时，用此字段作 ID

    boolean readonly() default false; // 在form页面显示为无样式 会传值 样式为readonly

    boolean required() default false;

    long min() default Long.MIN_VALUE;

    long max() default Long.MAX_VALUE;

    int min_length() default -1;

    int max_length() default Integer.MAX_VALUE;

    boolean host() default false;

    boolean port() default false;

    boolean email() default false; // 填值须是一个合法的邮箱地址

    boolean file() default false;

    String pattern() default ""; // 填值须要符合此正则表达式规则

    // 列表类型的相关信息

    boolean add() default true; // 支持创建页面输入

    boolean update() default true; // 支持编辑修改

    boolean show() default true; // 是否显示在 show 中。

    boolean list() default false; // 是否显示在列表中。

    boolean search() default false; // 是否支持列头搜索

    boolean numeric() default false;  // 该属性为监视类型中的动态数字类型，可用于绘制折线图。在该属性为监视类型时有效。

    ChartType chart_type() default ChartType.line; // 监视字段的图表类型，仅在 numeric=true 时有效

    String[] chart_group() default {}; // 图表字段的分组，一个字段可同时属于多个图表分组。

    String[] group() default {}; // 表单字段的分组，国际化数组格式。

    String[] color() default {}; // 用于样式转换，形式：{"当前字段值:#f7f7f7", "当前字段值:#xxxxxx"}

    int width_percent() default -1;

    // 字段的国际化名称数组，用于多语言环境下展示在用户界面。
    String[] name();

    // 字段的国际化描述信息数组，用于提供字段的详细说明信息。
    String[] info() default {};
}
