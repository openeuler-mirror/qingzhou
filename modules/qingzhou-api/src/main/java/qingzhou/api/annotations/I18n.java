package qingzhou.api.annotations;

import java.lang.annotation.*;

/**
 * 自定义国际化词条，供 {@code @ModelField.group}、{@code @ModelField.chart_title} 等按 code 引用，
 * 承载那些不属于任何注解自身的文案（如表单分组标题、图表标题）。
 * <p>
 * 可重复标注，全部词条随应用元数据一次性下发界面。
 */
@Repeatable(I18ns.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface I18n {
    /**
     * 引用键，须与引用方的取值一致。
     */
    String code();

    /**
     * 词条文案，国际化数组。
     */
    String[] name();
}
