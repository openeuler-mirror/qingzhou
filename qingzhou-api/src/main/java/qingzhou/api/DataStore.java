package qingzhou.api;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 用于 framework api 中各种类型的 Model 默认数据来源，注：各Model可能覆写方法，而不一定会使用 DataStore 中的数据，所以请勿在 框架内通过该对象进行数据相关操作
 */
public interface DataStore {
    // 加载全部数据，性能差，尽量使用下面的方法
    List<Map<String, String>> getAllData(String type) throws Exception;

    /***** 添加 *****/
    void addData(String type, String id, Map<String, String> properties) throws Exception;

    /***** 更新 *****/
    void updateDataById(String type, String id, Map<String, String> data) throws Exception;

    /***** 删除 *****/
    void deleteDataById(String type, String id) throws Exception;

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

    default List<String> getDataIdInPage(String type, int pageSize, int pageNum) throws Exception {
        List<String> allDataId = getAllDataId(type);
        int from = pageSize * (pageNum - 1);
        int to = pageSize * pageNum - 1;
        if (from < 0) from = 0;
        if (to > allDataId.size() - 1) to = allDataId.size() - 1;

        List<String> result = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            result.add(allDataId.get(i));
        }
        return result;
    }

    default Map<String, String> getDataById(String type, String id) throws Exception {
        for (Map<String, String> data : getAllData(type)) {
            if (Objects.equals(data.get(ListModel.FIELD_NAME_ID), id)) {
                return data;
            }
        }
        return null;
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
}
