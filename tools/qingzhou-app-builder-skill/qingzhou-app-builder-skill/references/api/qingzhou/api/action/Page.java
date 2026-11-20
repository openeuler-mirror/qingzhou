package qingzhou.api.action;

import java.util.List;
import java.util.Map;

/**
 * 列表分页的能力，对应内置动作 {@code page}。
 * <p>
 * 框架只提供分页参数与列定义，数据来源可以是库表、文件或实时探测结果；分页与字段裁剪的判定权在本接口，
 * 框架不再对返回的数据做二次排序或过滤。
 */
public interface Page {
    /**
     * @param pageNum    页码，从 1 开始，缺省 1
     * @param pageSize   每页条数，缺省 10，上限 100
     * @param query      查询条件，只包含 {@code search=true} 且值非空的字段
     * @param listFields 需返回的列，由框架按 {@code list=true} 的表单字段按声明顺序生成
     * @return 每行一个数组，元素须与 {@code listFields} 严格逐位对应，否则整列错位
     */
    List<String[]> page(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception;

    /**
     * 满足查询条件的总条数，框架在取到分页数据后调用以渲染分页器。数据量小或无法计数时保持默认实现即可，
     * 以避免额外的 count 查询。
     *
     * @return 返回 {@code -1} 表示数据不分页，前端不渲染分页器
     */
    default int total(Map<String, String> query) {
        return -1; // -1：不使用分页
    }

    /**
     * 判断记录是否存在。当前框架未调用，属预留能力，实现与否不影响列表展示。
     */
    boolean contains(String id);
}
