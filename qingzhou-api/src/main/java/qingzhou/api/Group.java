package qingzhou.api;

import java.util.Arrays;

/**
 * 用于表示页面表单字段的分组，该分组具有名称和国际化信息。
 */
public interface Group {

    /**
     * 获取表单字段分组的名称。
     *
     * @return 表单字段分组的名称。
     */
    String name();

    /**
     * 获取表单字段分组的国际化信息数组。
     *
     * @return 表单字段分组的国际化信息数组，每个元素代表一种语言环境下的分组显示名称。
     */
    String[] i18n();

    /**
     * 根据提供的名称创建一个表单字段分组实例。
     *
     * @param name 表单字段分组的名称。
     * @return 返回一个新的Group实例，其中name方法返回指定的名称，并根据名称生成对应的国际化信息数组。
     */
    static Group of(String name) {
        return new Group() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String[] i18n() {
                // 根据语言环境为分组名称生成相应的国际化显示名称
                return Arrays.stream(Lang.values()).map(lang -> lang.name() + Lang.SEPARATOR + name).toArray(String[]::new);
            }
        };
    }

    /**
     * 根据提供的名称和国际化信息数组创建一个表单字段分组实例。
     *
     * @param name 表单字段分组的名称。
     * @param i18n 表单字段分组的国际化信息数组，已包含所有需要的语言环境下的显示名称。
     * @return 返回一个新的Group实例，其中name方法返回指定的名称，i18n方法返回指定的国际化信息数组。
     */
    static Group of(String name, String[] i18n) {
        return new Group() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String[] i18n() {
                return i18n;
            }
        };
    }
}

