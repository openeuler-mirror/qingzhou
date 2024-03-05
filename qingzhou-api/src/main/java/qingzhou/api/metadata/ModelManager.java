package qingzhou.api.metadata;

import qingzhou.api.Group;
import qingzhou.api.Options;
import qingzhou.api.Request;

import java.util.Map;

/**
 * ModelManager接口，提供了对模型及其属性、动作、字段等信息的管理功能。
 */
public interface ModelManager {

    /**
     * 获取所有模型名称.
     *
     * @return 一个包含所有模型名称的字符串数组
     */
    String[] getModelNames();

    /**
     * 根据模型名称获取其默认属性.
     *
     * @param modelName 指定的模型名称
     * @return 一个映射表，键为属性名，值为属性默认值
     */
    Map<String, String> getModelDefaultProperties(String modelName);

    /**
     * 根据模型名称获取模型数据.
     *
     * @param modelName 需要获取模型数据的模型名称
     * @return 返回一个ModelData对象，包含了指定模型的数据
     */
    ModelData getModel(String modelName);

    /**
     * 获取指定模型的所有动作名称.
     *
     * @param modelName 模型的名称
     * @return 包含该模型所有动作名称的字符串数组
     */
    String[] getActionNames(String modelName);

    /**
     * 获取指定模型支持批量操作的动作名称.
     *
     * @param modelName 模型的名称
     * @return 包含该模型支持批量操作的动作名称的字符串数组
     */
    String[] getActionNamesSupportBatch(String modelName);

    /**
     * 根据模型名称和动作名称获取模型动作数据.
     *
     * @param modelName   模型的名称
     * @param actionName  动作的名称
     * @return 返回一个ModelActionData对象，表示指定模型和动作的数据
     */
    ModelActionData getModelAction(String modelName, String actionName);

    /**
     * 获取指定模型的所有字段名称.
     *
     * @param modelName 模型的名称
     * @return 包含该模型所有字段名称的字符串数组
     */
    String[] getFieldNames(String modelName);

    /**
     * 获取指定模型的所有分组名称.
     *
     * @param modelName 模型的名称
     * @return 包含该模型所有分组名称的字符串数组
     */
    String[] getGroupNames(String modelName);

    /**
     * 根据模型名称和分组名称获取属于该分组的字段名称列表.
     *
     * @param modelName   模型的名称
     * @param groupName   分组的名称
     * @return 返回一个字符串数组，包含属于指定分组的字段名称
     */
    String[] getFieldNamesByGroup(String modelName, String groupName);

    /**
     * 根据模型名称和分组名称获取分组详细信息
     *
     * @param modelName 模型的名称
     * @param groupName 分组的名称
     * @return 返回一个Group对象，表示指定模型和分组的数据
     */
    Group getGroup(String modelName, String groupName);

    /**
     * 根据模型名称和字段名称获取模型字段详细信息.
     *
     * @param modelName 模型的名称
     * @param fieldName 字段的名称
     * @return 返回一个ModelFieldData对象，包含了指定模型和字段的数据
     */
    ModelFieldData getModelField(String modelName, String fieldName);

    /**
     * 获取指定模型名称的监控字段映射。
     *
     * @param modelName 模型的名称，用于指定要获取字段映射的模型。
     * @return 返回一个映射，其中键是字段名，值是包含字段数据的 ModelFieldData 对象。
     */
    Map<String, ModelFieldData> getMonitorFieldMap(String modelName);

    /**
     * 根据请求和模型字段信息，获取配置选项。
     *
     * @param request 用户的请求对象，可能包含影响选项选择的参数。
     * @param modelName 模型的名称，指定选项查询的上下文模型。
     * @param fieldName 字段的名称，指定选项查询的具体字段。
     * @return 返回一个 Options 对象，包含根据请求和模型字段计算出的配置选项。
     */
    Options getOptions(Request request, String modelName, String fieldName);
}
