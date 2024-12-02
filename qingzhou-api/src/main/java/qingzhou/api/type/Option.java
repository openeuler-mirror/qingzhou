package qingzhou.api.type;

import qingzhou.api.Item;

/**
 * 定义了选项字段
 */

public interface Option {
    String ACTION_OPTION = "option";
    String FIELD_NAME_PARAMETER = "FIELD_NAME_PARAMETER";

    /**
     * 获取静态选项字段
     *
     * @return 返回静态选项字段的字符串数组
     */
    String[] staticOptionFields();

    /**
     * 获取动态选项字段
     *
     * @return 返回动态选项字段的字符串数组
     */
    String[] dynamicOptionFields();

    /**
     * 根据字段名称，将选项数据组织成对应的页面表单字段项目
     *
     * @return 返回表单字段项目的数组，其中表单字段项目可包含名称或国际化信息数组
     */
    Item[] optionData(String id, String fieldName);
}
