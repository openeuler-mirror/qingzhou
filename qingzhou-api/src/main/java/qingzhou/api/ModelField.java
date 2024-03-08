package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>该注解用于标注模型字段，应用于字段级别，并在运行时提供对字段的检索和处理能力。</p>
 *
 * <p>注意：此注解不支持原数据查询，如唯一性校验等需要比对源库数据的操作。
 * 这类逻辑不应在此注解中实现，而应在具体的业务逻辑Action中处理。</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelField {

    /**
     * 设置字段所属的页面表单展示分组，默认为空字符串表示无分组。
     *
     * @return 字段所属的组名
     */
    String group() default "";

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

    /**
     * 标识字段是否为必填项，默认为false。
     *
     * @return true表示必填，false表示非必填
     */
    boolean required() default false;

    /**
     * 设置字段的数据类型，默认为文本类型（FieldType.text）。
     *
     * @return 字段的数据类型
     */
    FieldType type() default FieldType.text;

    /**
     * 定义引用的模型。
     *
     * @return 返回引用的模型名称，默认为空字符串。
     */
    String refModel() default "";

    /**
     * 最小值。
     *
     * @return 返回最小值的长整型，默认为-1，代表无特定最小值限制。
     */
    long min() default -1;

    /**
     * 最大值。
     *
     * @return 返回最大值的长整型，默认为10亿，用以表示较大的上限值。
     */
    long max() default 1000000000;

    /**
     * 最小长度。
     *
     * @return 返回最小长度的整型，默认为0，代表无特定长度限制。
     */
    int minLength() default 0;

    /**
     * 最大长度。
     *
     * @return 返回最大长度的整型，默认为1000，代表允许的最大长度。
     */
    int maxLength() default 1000;

    /**
     * 是否为IP地址或主机名。
     *
     * @return 返回一个布尔值，标识是否是IP地址或主机名，默认为false。
     */
    boolean isIpOrHostname() default false;

    /**
     * 是否为通配符IP地址。
     *
     * @return 返回一个布尔值，标识是否是通配符IP地址，默认为false。
     */
    boolean isWildcardIp() default false;

    /**
     * 是否为端口号。
     *
     * @return 返回一个布尔值，标识是否是端口号，默认为false。
     */
    boolean isPort() default false;

    /**
     * 是否支持模式匹配。
     *
     * @return 返回一个布尔值，标识是否支持模式匹配，默认为false。
     */
    boolean isPattern() default false;

    /**
     * 是否为URL。
     *
     * @return 返回一个布尔值，标识是否是URL，默认为false。
     */
    boolean isURL() default false;

    /**
     * 字段值不为负数。如果没有指定此注解，则默认值为空字符串。
     *
     * @return 返回一个字符串，表示校验失败时的错误信息。
     */
    String noGreaterThanMinusOne() default "";

    /**
     * 字段值不能大于给定的值。如果没有指定此注解，则默认值为空字符串。
     *
     * @return 返回一个字符串，表示校验失败时的错误信息。
     */
    String noGreaterThan() default "";

    /**
     * 字段值不能大于等于给定的日期。如果没有指定此注解，则默认值为空字符串。
     *
     * @return 返回一个字符串，表示校验失败时的错误信息。
     */
    String noGreaterOrEqualThanDate() default "";

    /**
     * 字段值不能小于给定的值。如果没有指定此注解，则默认值为空字符串。
     *
     * @return 返回一个字符串，表示校验失败时的错误信息。
     */
    String noLessThan() default "";

    /**
     * 字段值不能小于等于给定的日期。如果没有指定此注解，则默认值为空字符串。
     *
     * @return 返回一个字符串，表示校验失败时的错误信息。
     */
    String noLessOrEqualThanDate() default "";

    /**
     * 字段值不能小于当前时间。默认为不进行此校验（false）。如果设置为true，则进行校验。
     *
     * @return 返回一个布尔值，表示是否进行字段值小于当前时间的校验。
     */
    boolean noLessThanCurrentTime() default false;

    /**
     * 字段值不能包含不支持的字符。如果没有指定此注解，则默认值为空字符串。
     *
     * @return 返回一个字符串，表示校验失败时的错误信息。
     */
    String notSupportedCharacters() default "";

    /**
     * 字段值不能为数组中任意一个不支持的字符串。如果没有指定此注解，则默认为空数组。
     *
     * @return 返回一个字符串数组，包含不支持的字符串。
     */
    String[] notSupportedStrings() default {};

    /**
     * 检查是否不支持中文字符。
     * 默认值为false，表示支持中文字符。
     *
     * @return 返回true表示不支持中文字符，false则表示支持。
     */
    boolean noSupportZHChar() default false;

    /**
     * 字段值不能与某个值相同。
     * 默认为空字符串，表示无特定限制。
     *
     * @return 返回一个字符串，指定不能与之相同的值。
     */
    String cannotBeTheSameAs() default "";

    /**
     * 是否跳过安全检查。
     * 默认为false，表示不跳过。
     *
     * @return 返回true表示跳过安全检查，false则进行安全检查。
     */
    boolean skipSafeCheck() default false;

    /**
     * 指定跳过字符检查的条件。
     * 默认为空字符串，表示无特定条件。
     *
     * @return 返回一个字符串，指定跳过字符检查的条件。
     */
    String skipCharacterCheck() default "";

    /**
     * 是否开启XSS Level 1检查。
     * 默认为false，表示不开启。
     *
     * @return 返回true表示开启XSS Level 1检查，false则表示不开启。
     */
    boolean checkXssLevel1() default false;

    /**
     * 是否启用客户端加密。
     * 默认为false，表示不启用。
     *
     * @return 返回true表示启用客户端加密，false则表示不启用。
     */
    boolean clientEncrypt() default false;

    /**
     * 指定生效条件。
     * 默认为空字符串，表示始终生效。
     *
     * @return 返回一个字符串，指定生效的条件。
     */
    String effectiveWhen() default "";

    /**
     * 创建时是否禁用。
     * 默认为false，表示不禁用。
     *
     * @return 返回true表示在创建时禁用，false则表示不禁用。
     */
    boolean disableOnCreate() default false;

    /**
     * 编辑时是否禁用。
     * 默认为false，表示不禁用，即可见但不可编辑。
     *
     * @return 返回true表示在编辑时禁用，false则表示不禁用。
     */
    boolean disableOnEdit() default false;

    /**
     * 编辑是否显示。
     * 默认为true，表示编辑时显示。
     *
     * @return 返回true表示编辑时显示，false则表示不可见。
     */
    boolean showToEdit() default true;

    /**
     * 是否显示在列表中。
     * 默认为false，表示不显示。
     *
     * @return 返回true表示在列表中显示，false则表示不显示。
     */
    boolean showToList() default false;

    /**
     * linkModel方法用于标注需要跳转到其他页面的链接字段。
     * 该字段的格式为 linkModel="model.action.field"，其中model和action定义了跳转逻辑，field为链接携带的参数key。
     * 该方法默认返回一个空字符串，表示没有链接字段需要标注。
     */
    String linkModel() default "";

    /**
     * valueFrom方法用于指定字段的值来源。
     * 该方法默认返回一个空字符串，表示字段的值不需要从其他地方获取。
     */
    String valueFrom() default "";

    /**
     * isMonitorField方法用于标记字段是否需要被监控。
     * 该方法默认返回false，表示字段不需要被监控。
     * 当设置为true时，表示该字段的值变化需要被监控。
     */
    boolean isMonitorField() default false;

    /**
     * supportGraphical方法用于标记字段的值是否是持续变化的整数，进而决定是否需要在画图板上进行绘图显示。
     * 该方法默认返回false，表示字段的值不需要在画图板上绘图显示。
     * 当设置为true时，表示字段的值是持续变化的整数，适合在画图板上进行动态显示。
     */
    boolean supportGraphical() default false;

    /**
     * supportGraphicalDynamic方法用于标记字段是否为动态多值的监视量。
     * 例如，在一个应用中对多个EJB实例的监视。
     * 该方法默认返回false，表示字段不是动态多值的监视量。
     * 当设置为true时，表示字段代表的是动态变化的多值监视量。
     * 注意：该属性与supportGraphical只能选择其一进行设置。
     */
    boolean supportGraphicalDynamic() default false;

}
