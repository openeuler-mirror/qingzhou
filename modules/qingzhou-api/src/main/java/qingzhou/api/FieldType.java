package qingzhou.api;

/**
 * 字段用途分类，决定字段进入哪条数据通道。
 */
public enum FieldType {
    /**
     * 业务字段：参与表单、列表与搜索，框架据此筛选 {@code add}/{@code update}/{@code show}/{@code page} 读写的字段。
     */
    form,
    /**
     * 监视字段：只用于监视页展示与指标导出，不参与表单与列表。
     */
    monitor
}
