package qingzhou.framework.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface DataStore {
    List<Map<String, String>> getAllData(String type) throws Exception;// 加载全部数据，性能差，尽量使用下面的方法

    default List<String> getAllDataId(String type) throws Exception {
        List<String> ids = new ArrayList<>();
        getAllData(type).forEach(data -> ids.add(data.get(ListModel.FIELD_NAME_ID)));
        return ids;
    }

    default boolean exists(String type, String id) throws Exception {
        return getAllDataId(type).contains(id);
    }

    default int getTotalSize(String type) throws Exception {
        return getAllDataId(type).size();
    }

    /***** 添加 *****/

    void addData(String type, String id, Map<String, String> properties) throws Exception;

    /***** 读取 *****/

    List<String> getDataIdInPage(String type, int pageSize, int pageNum) throws Exception;

    default Map<String, String> getDataById(String type, String id) throws Exception {
        return getAllData(type).stream().filter(data -> data.get(ListModel.FIELD_NAME_ID).equals(id)).findAny().get();
    }

    default List<Map<String, String>> getDataByIds(String type, String[] ids) throws Exception {
        return getAllData(type).stream().filter(data -> {
            for (String id : ids) {
                if (id.equals(data.get(ListModel.FIELD_NAME_ID))) return true;
            }
            return false;
        }).collect(Collectors.toList());
    }

    default List<Map<String, String>> getDataFieldByIds(String type, String[] ids, String[] fields) throws Exception {
        return getDataByIds(type, ids).stream().filter(new Predicate<Map<String, String>>() {
            private final List<String> listToShow = Arrays.asList(fields);

            @Override
            public boolean test(Map<String, String> data) {
                for (String k : data.keySet().toArray(new String[0])) {
                    if (!listToShow.contains(k)) {
                        data.remove(k);
                    }
                }
                return true;
            }
        }).collect(Collectors.toList());
    }

    default List<Map<String, String>> getDataByFieldLike(String type, String field, String likeValue) throws Exception {
        return getAllData(type).stream().filter(data -> data.get(field).toLowerCase().contains(likeValue.toLowerCase())).collect(Collectors.toList());
    }

    /***** 更新 *****/

    void updateDataById(String type, String id, Map<String, String> data) throws Exception;

    /***** 删除 *****/

    void deleteDataById(String type, String id) throws Exception;
}
