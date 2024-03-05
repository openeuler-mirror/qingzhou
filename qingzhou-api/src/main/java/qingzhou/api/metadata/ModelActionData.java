package qingzhou.api.metadata;

/**
 * ModelActionData 接口定义了模型操作的数据结构。
 * 该接口用于描述在模型操作中需要用到的数据，包括操作的名称、图标、国际化名称和信息、生效条件、转发页面、
 * 是否在列表头部显示、是否在列表中显示、在列表中的排序、是否支持批量操作以及是否禁用等属性。
 */
public interface ModelActionData {
    /**
     * 获取操作的名称。
     * @return 返回操作的名称，通常是一个字符串。
     */
    String name();

    /**
     * 获取操作图标的路径或标识。
     * @return 返回操作图标的路径或标识，通常是一个字符串。
     */
    String icon();

    /**
     * 获取操作名称的国际化资源数组。
     * @return 返回一个包含操作名称国际化资源的字符串数组。
     */
    String[] nameI18n();

    /**
     * 获取操作描述信息的国际化资源数组。
     * @return 返回一个包含操作描述信息国际化资源的字符串数组。
     */
    String[] infoI18n();

    /**
     * 获取操作生效的条件表达式。
     * @return 返回一个描述操作生效条件的字符串。
     */
    String effectiveWhen();

    /**
     * 获取操作执行后转发的页面路径。
     * @return 返回一个字符串，表示操作执行后页面转发的路径。
     */
    String forwardToPage();

    /**
     * 判断操作是否显示在列表头部。
     * @return 如果操作应该显示在列表头部，则返回true，否则返回false。
     */
    boolean showToListHead();

    /**
     * 判断操作是否显示在列表中。
     * @return 如果操作应该显示在列表中，则返回true，否则返回false。
     */
    boolean showToList();

    /**
     * 获取操作在列表中的排序位置。
     * @return 返回一个整数，表示操作在列表中的排序顺序。
     */
    int orderOnList();

    /**
     * 判断操作是否支持批量操作。
     * @return 如果操作支持批量操作，则返回true，否则返回false。
     */
    boolean supportBatch();

    /**
     * 判断操作是否被禁用。
     * @return 如果操作被禁用，则返回true，否则返回false。
     */
    boolean disabled();
}

