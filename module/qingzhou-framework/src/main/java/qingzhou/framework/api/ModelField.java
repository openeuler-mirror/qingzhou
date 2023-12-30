package qingzhou.framework.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelField {
    String[] nameI18n();

    String[] infoI18n();

    /**
     * 标注需要在页面上按顺序显示，自然顺序排序
     */
    int order() default -1;

    FieldType type() default FieldType.text;

    Class<?> refModel() default Object.class;

    boolean allowRefModelEdit() default true;

    boolean allowRefModelDelete() default false;

    boolean required() default false;

    boolean unique() default false;

    long min() default -1;

    long max() default 1000000000;// 通道的最大Post字节 500MB 会超过一个亿，因此这里设置 10 亿

    int minLength() default 0;

    int maxLength() default 1000;

    boolean isIpOrHostname() default false;

    boolean isWildcardIp() default false;

    boolean isFileName() default false;

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

    String[] mustStartsWith() default {};

    boolean skipIdFormat() default false;

    boolean skipSafeCheck() default false;

    String skipCharacterCheck() default "";

    boolean checkXssLevel1() default false;

    String valueFrom() default "";

    boolean clientEncrypt() default false;

    String group() default "";

    String effectiveWhen() default "";

    boolean effectiveOnCreate() default true;

    boolean effectiveOnEdit() default true;

    boolean showToEdit() default true;

    boolean showToShow() default true;

    boolean showToList() default false;

    /**
     * 标注需要跳转到其他页面的链接字段，不要标注在id字段上，格式为 linkModel="modelname.action.fieldname"，根据modelname和action跳转，fieldname为链接携带的参数key
     */
    String linkModel() default "";
}
