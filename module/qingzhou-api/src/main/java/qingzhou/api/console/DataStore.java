package qingzhou.api.console;

import java.util.List;
import java.util.Map;

public interface DataStore {
    boolean exists(String type, String id) throws Exception;

    List<String> getAllDataId(String type) throws Exception;

    List<Map<String, String>> getAllData(String type) throws Exception;

    int getTotalSize(String type) throws Exception;

    List<String> getPageDataId(String type, String orderBy, boolean ascend, int pageSize, int pageNum) throws Exception;

    List<Map<String, String>> listByPage(String type, int start, int size, String orderBy, boolean ascend) throws Exception;

    /***** 添加 *****/

    void addData(String type, String id, Map<String, String> properties) throws Exception;

    /***** 读取 *****/

    Map<String, String> getDataById(String type, String id) throws Exception;

    List<Map<String, String>> getDataByKey(String type, String key, String value) throws Exception;

    Map<String, String> getSpecifiedData(String type, String id, String[] specifiedKeys) throws Exception;

    /***** 更新 *****/

    void updateDataById(String type, String id, Map<String, String> properties) throws Exception;

    void updateSpecifiedData(String type, String id, Map<String, String> specifiedProperties) throws Exception;

    /***** 删除 *****/

    void deleteDataById(String type, String id) throws Exception;

    void deleteSpecifiedData(String type, String id, String[] specifiedKeys) throws Exception;
}
