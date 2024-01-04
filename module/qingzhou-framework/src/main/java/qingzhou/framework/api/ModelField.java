package qingzhou.framework.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelField { // todo 内容太多，可以按照 Validation、Monitor 分为不同的注解？
    String group() default "";

    String[] nameI18n();

    String[] infoI18n();

    boolean required() default false;

    FieldType type() default FieldType.text;

    String refModel() default "";

    long min() default -1;

    long max() default 1000000000;// 通道的最大Post字节 500MB 会超过一个亿，因此这里设置 10 亿

    int minLength() default 0;

    int maxLength() default 1000;

    boolean isIpOrHostname() default false;

    boolean isWildcardIp() default false;

    boolean isPort() default false;

    boolean isPattern() default false;

    boolean isURL() default false;

    String noGreaterThanMinusOne() default "";

    String noGreaterThan() default "";

    String noGreaterOrEqualThanDate() default "";

    String noLessThan() default "";

    String noLessOrEqualThanDate() default "";

    boolean noLessThanCurrentTime() default false;

    String notSupportedCharacters() default "";

    String[] notSupportedStrings() default {};

    boolean noSupportZHChar() default false;

    String cannotBeTheSameAs() default "";

    boolean skipSafeCheck() default false;

    String skipCharacterCheck() default "";

    boolean checkXssLevel1() default false;

    boolean clientEncrypt() default false;

    String effectiveWhen() default "";

    boolean disableOnCreate() default false;

    boolean disableOnEdit() default false; // 可见，不可编辑

    boolean showToEdit() default true; // 不可见

    boolean showToList() default false;

    /**
     * 标注需要跳转到其他页面的链接字段，不要标注在id字段上，格式为 linkModel="modelname.action.fieldname"，根据modelname和action跳转，fieldname为链接携带的参数key
     */
    String linkModel() default "";

    String valueFrom() default "";

    boolean isMonitorField() default false;

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
