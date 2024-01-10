package qingzhou.framework.api;

/**
 * 用于指示模块的属性在 HTML 网页上的显示样式
 */
public enum FieldType {
    textarea,           // 多行文本框
    number,             // 整数文本框
    decimal,            // 浮点数文本框
    password,           // 密码文本框
    radio,              // 单选框
    bool,               // 开关
    select,             // 下拉框
    multiselect,        // 多选下拉框
    groupedMultiselect, // 多选下拉框（带分组）
    sortable,           // 可排序输入框
    checkbox,           // 复选框
    sortableCheckbox,   // 可排序复选框
    file,               // 单文件上传
    selectCharset,      // 选择支持的字符集中的一个
    kv,                 // key-value 输入框
    datetime,           // 日期选择
    text                // 默认值，保持在最后一个比较整齐
}