package qingzhou.api;

import java.util.Arrays;

/**
 * Option接口定义了选项的基本行为，包括获取选项值和国际化字符串数组。
 */
public interface Option {
    /**
     * 获取选项的值。
     * 该方法不接受任何参数，返回选项的当前值。
     *
     * @return 返回选项的值，类型为String。
     */
    String value();

    /**
     * 获取选项的国际化字符串数组。
     * 该方法用于获取选项的多语言支持字符串，适用于需要国际化展示的选项。
     *
     * @return 返回一个String数组，包含选项的各种语言版本。
     */
    String[] i18n();

    /**
     * 根据提供的值创建一个Option实例。
     * @param value 选项的值。
     * @return 返回一个新的Option实例，其value方法返回指定的值，i18n方法返回基于Lang枚举生成的国际化字符串数组。
     */
    static Option of(String value) {
        return new Option() {
            @Override
            public String value() {
                return value;
            }

            @Override
            public String[] i18n() {
                // 为每个语言生成对应的国际化字符串
                return Arrays.stream(Lang.values()).map(lang -> lang.name() + Lang.SEPARATOR + value).toArray(String[]::new);
            }
        };
    }

    /**
     * 根据提供的值和国际化字符串数组创建一个Option实例。
     * @param value 选项的值。
     * @param i18n 选项的国际化字符串数组。
     * @return 返回一个新的Option实例，其value方法返回指定的值，i18n方法返回指定的国际化字符串数组。
     */
    static Option of(String value, String[] i18n) {
        return new Option() {
            @Override
            public String value() {
                return value;
            }

            @Override
            public String[] i18n() {
                return i18n;
            }
        };
    }
}

