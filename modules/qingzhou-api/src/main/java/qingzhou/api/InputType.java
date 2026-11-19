package qingzhou.api;

/**
 * 字段的控件形态，决定表单如何渲染与采集该字段，以及后端按何种规则校验。
 * <p>
 * 取值在语义上均为字符串；多值字段统一按 {@code @ModelField.separator} 连接成一个参数值传递。
 */
public enum InputType {
    /**
     * 单行文本。
     */
    text,
    /**
     * 多行文本。
     */
    textarea,
    /**
     * 口令，界面掩码显示。
     */
    password,
    /**
     * 整数。
     */
    number,
    /**
     * 小数。
     */
    decimal,
    /**
     * 单个时间点。
     */
    datetime,
    /**
     * 时间区间，起止值按分隔符连接为一个参数。
     */
    range_datetime,
    /**
     * 文件，请求按 multipart 解析，参数值是服务端临时路径，需长期保留的文件由应用自行搬移。
     */
    file,
    /**
     * 布尔开关，取值 {@code true}/{@code false}。
     */
    bool,
    /**
     * 单选，候选项来自 {@code @ModelField.options}。
     */
    radio,
    /**
     * 多选，候选项来自 {@code @ModelField.options}，选中值按分隔符连接。
     */
    checkbox,
    /**
     * 面向有序集合的多选，选中值按分隔符连接。
     */
    sortable_checkbox,
    /**
     * 下拉单选，可配合 {@code ref_model} 从其他模块动态取候选。
     */
    select,
    /**
     * 下拉多选，选中值按分隔符连接。
     */
    multiselect,
    /**
     * 可拖拽排序的列表，增删项与顺序由用户维护，最终按分隔符连接成一个值。
     */
    sortable,
    /**
     * 键值对文本，形如 {@code k1=v1,k2=v2}，结构由应用自行解析与校验。
     */
    kv,
    /**
     * Markdown 文本，编辑时实时预览。
     */
    markdown,
    /**
     * 复合输入：把一个逻辑字段拆成多个子输入框分别填写，提交时按分隔符合并成单值。
     */
    combine,
    /**
     * 分组多选，候选项先按组归类再整体多选。
     */
    grouped_multiselect
}
