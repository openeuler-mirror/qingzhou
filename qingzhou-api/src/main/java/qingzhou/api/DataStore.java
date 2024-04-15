package qingzhou.api;

import qingzhou.api.type.Listable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Model可能覆写方法，而不一定会使用 DataStore 中的数据，所以请勿在框架内通过该对象进行数据相关操作
 */
public interface DataStore {
    /**
     * 获取指定类型的所有数据。由于性能问题，应尽量避免使用该方法。
     *
     * @param type 数据类型标识
     * @return 包含所有指定类型数据的列表，每条数据以 Map 格式表示
     * @throws Exception 操作失败时抛出的异常
     */
    List<Map<String, String>> getAllData(String type) throws Exception;

    /**
     * 添加指定类型的数据。
     *
     * @param type       数据类型标识
     * @param id         数据的唯一标识
     * @param properties 数据的属性，以键值对形式表示
     * @throws Exception 添加失败时抛出的异常
     */
    void addData(String type, String id, Map<String, String> properties) throws Exception;

    /**
     * 根据 ID 更新指定类型的数据。
     *
     * @param type 数据类型标识
     * @param id   数据的唯一标识
     * @param data 要更新的数据，以键值对形式表示
     * @throws Exception 更新失败时抛出的异常
     */
    void updateDataById(String type, String id, Map<String, String> data) throws Exception;

    /**
     * 根据 ID 删除指定类型的数据。
     *
     * @param type 数据类型标识
     * @param id   数据的唯一标识
     * @throws Exception 删除失败时抛出的异常
     */
    void deleteDataById(String type, String id) throws Exception;

    /**
     * 获取指定类型的所有数据的 ID 列表。
     *
     * @param type 数据类型标识
     * @return 所有数据的 ID 列表
     * @throws Exception 操作失败时抛出的异常
     */
    default List<String> getAllDataId(String type) throws Exception {
        List<String> ids = new ArrayList<>();
        getAllData(type).forEach(data -> ids.add(data.get(Listable.FIELD_NAME_ID)));
        return ids;
    }

    /**
     * 检查指定类型下是否存在指定 ID 的数据。
     *
     * @param type 数据类型标识
     * @param id   数据的唯一标识
     * @return 存在返回 true，否则返回 false
     * @throws Exception 操作失败时抛出的异常
     */
    default boolean exists(String type, String id) throws Exception {
        return getAllDataId(type).contains(id);
    }

    /**
     * 获取指定类型的数据总数。
     *
     * @param type 数据类型标识
     * @return 数据总数
     * @throws Exception 操作失败时抛出的异常
     */
    default int getTotalSize(String type) throws Exception {
        return getAllDataId(type).size();
    }

    /**
     * 根据页码和页面大小获取指定类型的数据 ID 列表。
     *
     * @param type     数据类型标识
     * @param pageSize 每页的数据数量
     * @param pageNum  页码
     * @return 该页的数据 ID 列表
     * @throws Exception 操作失败时抛出的异常
     */
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

    /**
     * 根据 ID 获取指定类型的数据。
     *
     * @param type 数据类型标识
     * @param id   数据的唯一标识
     * @return 对应的数据，如果不存在则返回 null
     * @throws Exception 操作失败时抛出的异常
     */
    default Map<String, String> getDataById(String type, String id) throws Exception {
        List<Map<String, String>> allData = getAllData(type);
        if (allData.size() == 1 && id == null) {
            if (!allData.get(0).containsKey(Listable.FIELD_NAME_ID)) {
                return allData.get(0);
            }
        }
        for (Map<String, String> data : allData) {
            if (Objects.equals(data.get(Listable.FIELD_NAME_ID), id)) {
                return data;
            }
        }
        return null;
    }

    /**
     * 根据一组 ID 获取指定类型的数据。
     *
     * @param type 数据类型标识
     * @param ids  数据的唯一标识数组
     * @return 包含指定 ID 数据的列表，每条数据以 Map 格式表示
     * @throws Exception 操作失败时抛出的异常
     */
    default List<Map<String, String>> getDataByIds(String type, String[] ids) throws Exception {
        return getAllData(type).stream().filter(data -> {
            for (String id : ids) {
                if (id.equals(data.get(Listable.FIELD_NAME_ID))) return true;
            }
            return false;
        }).collect(Collectors.toList());
    }

    /**
     * 根据一组 ID 和字段名获取指定类型的数据，仅包含指定的字段。
     *
     * @param type   数据类型标识
     * @param ids    数据的唯一标识数组
     * @param fields 需要获取的字段名数组
     * @return 包含指定 ID 数据的列表，每条数据以 Map 格式表示，仅包含指定的字段
     * @throws Exception 操作失败时抛出的异常
     */
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
}

