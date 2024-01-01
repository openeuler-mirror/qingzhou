package qingzhou.framework.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MonitorField {
    /**
     * 名称 国际化(注：国际化语言不指定视为中文)：示例：{"zh:确定", "en:yes"}
     */
    String[] nameI18n();

    /**
     * 描述 国际化(注：国际化语言不指定视为中文)：示例：{"zh:确定", "en:yes"}
     */
    String[] infoI18n();

    /**
     * 是否是持续变化的整数，用于标记是否要在画图板上绘图显示。
     */
    boolean supportGraphical() default false;

    /**
     * 表示动态多值的监视量，如 一个应用里 多个ejb实例的监视
     * 只支持：持续变化的整数，用于标记是否要在画图板上绘图显示。
     * 和 supportGraphical 二选一
     */
    boolean supportGraphicalDynamic() default false;
}
