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
    // 字段的国际化名称数组，用于多语言环境下展示在用户界面。
    String[] name();

    // 字段的国际化描述信息数组，用于提供字段的详细说明信息。
    String[] info() default {};

    String group() default ""; // 设置字段所属的页面表单展示分组，默认为空字符串表示“其它”分组。

    FieldType field_type() default FieldType.FORM;

    // FieldType.FORM 表单校验

    InputType input_type() default InputType.text; // 字段的显示类型

    String display() default ""; // 在页面上显示或隐藏的条件

    boolean required() default false;

    long min() default Long.MIN_VALUE;

    long max() default Long.MAX_VALUE;

    int min_length() default -1;

    int max_length() default Integer.MAX_VALUE;

    boolean host() default false;

    boolean port() default false;

    String[] forbid() default {};

    String pattern() default ""; // 填值须要符合此正则表达式规则

    String[] xss_skip() default {};

    String separator() default ","; // 当使用 options() 或 refModel()，用以分割多值

    boolean email() default false; // 填值须是一个合法的邮箱地址

    boolean file() default false;

    boolean skip_validate() default false;  // 跳过轻舟框架校验

    String[] echo_group() default {};

    // 列表类型的相关信息

    boolean create() default true; // 支持创建页面输入

    boolean edit() default true; // 支持编辑修改

    String update_action() default ""; //标记该字段在list页面修改后走这个action

    boolean plain_text() default false; // 表单元素级联控制只读的事件绑定 不会传值 样式为无样式

    String placeholder() default "";

    boolean readonly() default false; // 在form页面显示为无样式 会传值 样式为readonly

    boolean show_label() default true;       // 表单页是否显示标签名

    boolean same_line() default false;

    boolean hidden() default false; // 字段是否隐藏

    boolean show() default true; // 是否显示在 show 中。

    boolean numeric() default false;  // 该属性为监视类型中的动态数字类型，可用于绘制折线图。在该属性为监视类型时有效。

    String[] combine_fields() default {};  // 组合条件属性

    boolean list() default false; // 是否显示在列表中。

    String order() default "5"; // 排序符，按自然顺序

    String[] color() default {}; // 用于样式转换，形式：{"当前字段值:#f7f7f7", "当前字段值:#xxxxxx"}

    int width_percent() default -1;

    int ignore() default -1; // 列表页面上，最多显示的字符数，超出后隐藏并悬浮显示全值

    Class<? extends ModelBase> ref_model() default ModelBase.class; // 使用指定的模块的所有数据id作为字段的取值范围

    String link_action() default ""; // 链接到模块内部的 action 上

    String link_model() default ""; // 链接到其他模块的 list action，格式：currentModelFieldName,xxxx,xxxx>targetModelName

    ActionType action_type() default ActionType.link; // 列表字段连接类型

    boolean search() default false; // 是否支持列头搜索

    boolean search_multiple() default false;// 当search为ture和本值为true的时候，显示为多选下拉搜索
}
