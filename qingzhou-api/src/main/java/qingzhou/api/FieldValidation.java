package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldValidation {
    /**
     * 标识字段是否为必填项，默认为false。
     *
     * @return true表示必填，false表示非必填
     */
    boolean required() default false;

    /**
     * 校验此字段须是数字类型可，不能小于此值，-1 表示无限制。
     */
    long numberMin() default Integer.MIN_VALUE;

    long numberMax() default Integer.MAX_VALUE;

    int lengthMin() default 0;

    /**
     * 须字符串类型，且长度不能超过此指数
     */
    int lengthMax() default 1000;

    boolean hostname() default false;

    boolean port() default false;

    /**
     * 字段值不能包含不支持的字符。如果没有指定此注解，则默认值为空字符串。
     *
     * @return 返回一个字符串，表示校验失败时的错误信息。
     */
    String unsupportedCharacters() default "";

    /**
     * 字段值不能为数组中任意一个不支持的字符串。如果没有指定此注解，则默认为空数组。
     *
     * @return 返回一个字符串数组，包含不支持的字符串。
     */
    String[] unsupportedStrings() default {};

    /**
     * 指定生效条件。
     * 默认为空字符串，表示始终生效。
     *
     * @return 返回一个字符串，指定生效的条件。
     */
    String effectiveWhen() default "";

    /**
     * 标记字段在创建时是不能输入
     */
    boolean cannotAdd() default false;

    /**
     * 标记字段不能更新
     */
    boolean cannotUpdate() default false;
}
