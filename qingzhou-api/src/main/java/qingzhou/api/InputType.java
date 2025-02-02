package qingzhou.api;

/**
 * 该枚举类型用于指示模块属性在HTML网页上显示的样式。
 * 每个枚举成员代表一种特定类型的HTML表单元素或组件，
 * 适用于不同的数据输入和展示需求。
 */
public enum InputType {
    text,               // 表示普通文本输入样式。
    textarea,           // 多行文本框，用户可在此输入多行文本内容。
    password,           // 密码文本框，用于输入密码，输入内容将以星号或其他符号隐藏。
    number,             // 整数文本框，仅允许输入整数值。
    decimal,            // 浮点数文本框，允许输入带小数点的数值。
    datetime,           // 日期时间选择器，用于选择日期和时间。
    range_datetime,     // 选择一段日期范围
    file,               // 文件上传控件，允许用户选择并上传单个文件。
    bool,               // 开关，表示布尔值状态（开启/关闭），通常用于切换功能的启用或禁用。
    radio,              // 单选框，提供一组选项，用户只能从中选择一个。
    checkbox,           // 复选框，提供一组复选框，用户可以勾选任意数量的选项。
    sortable_checkbox,  // 可排序复选框，与复选框类似，但支持通过拖放操作对选项进行排序。
    select,             // 下拉框，提供一个可下拉的选择列表，用户从中选择一项。
    multiselect,        // 多选下拉框，允许用户从列表中选择多个选项。
    sortable,           // 可排序输入框，允许用户通过拖放操作对输入内容进行排序。
    kv,                 // Key-Value 输入框，允许用户以键值对的形式输入数据。
    markdown,           // Markdown 样式，用于以Markdown格式显示文本内容。
    combine,
    grouped_multiselect //分组 多选下拉
}
