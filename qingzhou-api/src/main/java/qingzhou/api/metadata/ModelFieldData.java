package qingzhou.api.metadata;

import qingzhou.api.FieldType;

/**
 * ModelFieldData 接口定义了模型字段的各种数据属性和校验规则。
 */
public interface ModelFieldData {
    // 获取字段分组名称
    String group();

    // 获取字段名称的国际化数组
    String[] nameI18n();

    // 获取字段信息的国际化数组
    String[] infoI18n();

    // 判断字段是否为必填项
    boolean required();

    // 获取字段类型
    FieldType type();

    // 获取引用的模型名称
    String refModel();

    // 获取字段值的最小整数值
    long min();

    // 获取字段值的最大整数值
    long max();

    // 获取字段最小长度
    int minLength();

    // 获取字段最大长度
    int maxLength();

    // 判断字段值是否为IP地址或主机名
    boolean isIpOrHostname();

    // 判断字段值是否为通配符IP地址
    boolean isWildcardIp();

    // 判断字段值是否为端口号
    boolean isPort();

    // 判断字段值是否为模式匹配
    boolean isPattern();

    // 判断字段值是否为URL
    boolean isURL();

    // 获取字段值不能大于某个值的错误消息
    String noGreaterThanMinusOne();

    // 获取字段值不能大于某个指定值的错误消息
    String noGreaterThan();

    // 获取字段值不能大于等于某个日期的错误消息
    String noGreaterOrEqualThanDate();

    // 获取字段值不能小于某个值的错误消息
    String noLessThan();

    // 获取字段值不能小于等于某个日期的错误消息
    String noLessOrEqualThanDate();

    // 判断字段值是否不能小于当前时间
    boolean noLessThanCurrentTime();

    // 获取不支持的字符错误消息
    String notSupportedCharacters();

    // 获取不支持的字符串数组
    String[] notSupportedStrings();

    // 判断字段值是否不支持中文字符
    boolean noSupportZHChar();

    // 获取字段值不能与某个值相同的错误消息
    String cannotBeTheSameAs();

    // 判断是否跳过安全检查
    boolean skipSafeCheck();

    // 获取跳过字符检查的设置
    String skipCharacterCheck();

    // 判断是否进行XSS Level 1检查
    boolean checkXssLevel1();

    // 判断字段值是否需要客户端加密
    boolean clientEncrypt();

    // 获取字段值生效的条件表达式
    String effectiveWhen();

    // 判断字段是否在创建时禁用
    boolean disableOnCreate();

    // 判断字段是否在编辑时禁用
    boolean disableOnEdit();

    // 判断字段是否显示在编辑界面
    boolean showToEdit();

    // 判断字段是否显示在列表中
    boolean showToList();

    // 获取字段链接的模型名称
    String linkModel();

    // 获取字段值的来源表达式
    String valueFrom();

    // 判断字段是否为监控字段
    boolean isMonitorField();

    // 判断字段是否支持图形化展示
    boolean supportGraphical();

    // 判断字段是否支持动态图形化展示
    boolean supportGraphicalDynamic();
}

