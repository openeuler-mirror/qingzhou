package qingzhou.api.type;

import java.util.List;
import java.util.Map;

/**
 * 提供列表展示功能的接口，继承自Showable接口。
 */
public interface Listable extends Showable {
    String idFieldName();

    List<Map<String, String>> listData(int pageNum, int pageSize, String[] fieldNames) throws Exception;

    int totalSize();
}
