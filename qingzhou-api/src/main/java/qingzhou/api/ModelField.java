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
     * 以下是公共类型的信息
     */

    // 获取字段的国际化名称数组，用于多语言环境下展示在用户界面。
    String[] name();

    // 获取字段的国际化描述信息数组，用于提供字段的详细说明信息。
    String[] info() default {};

    String group() default ""; // 设置字段所属的页面表单展示分组，默认为空字符串表示“其它”分组。

    FieldType fieldType() default FieldType.FORM;

    /**
     * 以下是 FieldType.Form 类型的信息
     */

    InputType inputType() default InputType.text; // 字段的显示类型

    String display() default ""; // 允许指定值

    String readOnly() default ""; // 表单元素级联控制只读的事件绑定

    boolean required() default false;

    Class<? extends ModelBase> refModel() default ModelBase.class; // 使用指定的模块的所有数据id作为字段的取值范围

    String separator() default ","; // 当使用 options() 或 refModel()，用以分割多值

    long min() default Long.MIN_VALUE;

    long max() default Long.MAX_VALUE;

    int lengthMin() default -1;

    int lengthMax() default Integer.MAX_VALUE;

    boolean host() default false;

    boolean port() default false;

    String[] forbid() default {};

    String[] skip() default {};

    String pattern() default ""; // 填值须要符合此正则表达式规则

    boolean email() default false; // 填值须是一个合法的邮箱地址

    boolean file() default false;

    String[] echoGroup() default {};

    // 列表类型的相关信息

    boolean update() default false; // 支持直接在列表上进行编辑

    boolean show() default true; // 是否显示在列表中。

    boolean list() default false; // 是否显示在列表中。

    boolean search() default false; // 是否支持列头搜索

    boolean linkShow() default false; // 点击此字段调整到详情页面，模块须支持 show

    int widthPercent() default -1;

    int ignore() default -1; // 列表页面上，最多显示的字符数，超出后隐藏并悬浮显示全值

    // 标注需要跳转到其他页面的链接字段，不要标注在id字段上，格式为 linkModel="modelname.fieldname"，根据modelname跳转到固定action-list，fieldname为链接携带的参数key,即跳转后的搜索条件参数
    String linkList() default "";

    // 用于样式转换，形式：{"当前字段值:#f7f7f7", "当前字段值:#xxxxxx"}
    String[] color() default {};

    /**
     * 以下是 FieldType.Monitor 类型的信息
     */

    boolean numeric() default false;  // 该属性为监视类型中的动态数字类型，可用于绘制折线图。在该属性为监视类型时有效。
}
