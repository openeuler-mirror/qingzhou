package qingzhou.api;

/**
 * 监视字段的图表形态，仅在 {@code @ModelField.numeric = true} 时由界面按此渲染。
 */
public enum ChartType {
    /**
     * 折线图，适合随时间变化的时序指标。
     */
    line,
    /**
     * 柱状图，适合离散分类之间的比较。
     */
    bar,
    /**
     * 仪表盘，适合带阈值区间的单值指标。
     */
    gauge,
    /**
     * 数值卡片，只展示当前值，不绘图。
     */
    stat,
    /**
     * 饼图，适合构成占比。
     */
    pie
}
