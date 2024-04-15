package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MonitorField {
    /**
     * 获取字段的国际化名称数组，用于多语言环境下展示在用户界面。
     *
     * @return 字段国际化名称数组
     */
    String[] nameI18n();

    /**
     * 获取字段的国际化描述信息数组，用于提供字段的详细说明信息。
     *
     * @return 字段国际化描述信息数组
     */
    String[] infoI18n();

    boolean dynamic() default true;

    boolean dynamicMultiple() default false;
}
