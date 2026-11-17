package qingzhou.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import qingzhou.api.ChartType;
import qingzhou.api.FieldType;
import qingzhou.api.InputType;

/**
 * 声明一个模块字段。同一份声明被复用于表单控件、列表列、搜索条件、后端校验与接口参数，
 * {@link #code()} 是应用代码、请求参数、动作返回 Map 三方对齐的唯一键。
 * <p>
 * 只扫描 {@code public} 字段，且字段初值会作为表单默认值下发。校验只在 {@code add}/{@code update} 动作上执行，
 * 且只校验请求中出现的字段。本注解只描述结构，不具备源数据查询能力：
 * 唯一性校验、跨记录一致性等需要比对源数据的逻辑，必须在具体动作实现中完成。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface ModelField {
    /**
     * 数据 key，缺省为字段名。须与请求参数名、{@code Show}/{@code Monitor} 返回 Map 的 key 一致。
     */
    String code() default "";

    /**
     * 字段用途，决定进入表单通道还是监视通道。
     */
    FieldType field_type() default FieldType.form;

    /**
     * 控件形态，决定前端渲染与后端校验规则。
     */
    InputType input_type() default InputType.text;

    /**
     * 显示条件表达式，支持 {@code 字段}（非空即真）、{@code !字段}、{@code 字段==值}、{@code 字段!=值}，
     * 用 {@code &}/{@code &&}（优先级高）与 {@code |}/{@code ||} 组合，不支持括号与大小比较。
     * <p>
     * 空串恒真。它只控制界面显隐，并决定 {@link #required()} 是否生效，不构成权限。
     */
    String display() default "";

    /**
     * 候选取值来自指定模块的全部 id，用于需要引用其他模块数据的字段。
     */
    String ref_model() default "";

    /**
     * 点击单元格时跳转到指定模型的列表页并按其值筛选，格式为 {@code modelCode.fieldCode}。
     */
    String link_to() default "";

    /**
     * 候选项，同时充当取值白名单：提交值不在其中即校验失败；多值字段按 {@link #separator()} 拆分后逐个比对。
     */
    String[] options() default {};

    /**
     * 多值分隔符，用于多选类字段的存储与校验，也是复合控件合并成单值时的连接符。
     */
    String separator() default ",";

    /**
     * 标记为记录主键，须满足 {@code ^[a-zA-Z0-9_-]{1,20}$}。{@code update} 提交时会从数据中剔除该字段，
     * 界面也据此定位行与空间切换对象。
     */
    boolean id() default false;

    /**
     * 表单只读：界面禁止编辑，且不参与后端校验。
     */
    boolean readonly() default false;

    /**
     * 必填；仅在请求中未提交该字段、且 {@link #display()} 表达式为真时校验。
     */
    boolean required() default false;

    /**
     * 数值下界。
     */
    long min_value() default Long.MIN_VALUE;

    /**
     * 数值上界。
     */
    long max_value() default Long.MAX_VALUE;

    /**
     * 最小长度，{@code -1} 表示不限制。
     */
    int min_length() default -1;

    /**
     * 最大长度。
     */
    int max_length() default Integer.MAX_VALUE;

    /**
     * 按主机名或 IPv4 校验。
     */
    boolean host() default false;

    /**
     * 按端口号（1-65535）校验。
     */
    boolean port() default false;

    /**
     * 按邮箱格式校验。
     */
    boolean email() default false;

    /**
     * 文件字段标记。实际的上传判定以后端按 {@code input_type=file} 为准，仅置此项不会触发上传解析。
     */
    boolean file() default false;

    /**
     * 自定义正则校验，长度不超过 256 字符。
     */
    String pattern() default "";

    /**
     * 是否作为新增表单的字段。
     */
    boolean add() default true;

    /**
     * 是否作为更新表单的字段。
     */
    boolean update() default true;

    /**
     * 是否作为查看/详情页的字段。
     */
    boolean show() default true;

    /**
     * 是否作为列表列，列顺序即字段声明顺序。
     */
    boolean list() default false;

    /**
     * 是否作为查询条件；为真时该参数才会进入 {@code Page.query}。
     */
    boolean search() default false;

    /**
     * 标记为监视字段中的动态数值，可绘图并可导出为监控指标。
     */
    boolean numeric() default false;

    /**
     * 图表形态，仅在 {@link #numeric()} 为真时有效。
     */
    ChartType chart_type() default ChartType.line;

    /**
     * 所属表单分组，取值为 {@link I18n#code()}。
     */
    String group() default "";

    /**
     * 所属图表的标题，取值为 {@link I18n#code()}。
     */
    String chart_title() default "";

    /**
     * 值到颜色的样式映射，形式为 {@code {"字段值:#RRGGBB"}}，未命中时使用中性色。
     */
    String[] color() default {};

    /**
     * 列宽百分比，{@code -1} 表示由界面自适应。
     */
    int width_percent() default -1;

    /**
     * 字段名称，国际化数组。
     */
    String[] name();

    /**
     * 字段说明，国际化数组。
     */
    String[] info() default {};
}
