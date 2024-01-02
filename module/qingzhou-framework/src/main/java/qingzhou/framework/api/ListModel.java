package qingzhou.framework.api;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public interface ListModel extends ShowModel {
    String FIELD_NAME_ID = "id";
    String ACTION_NAME_LIST = "list";
    String PARAMETER_PAGE_NUM = "pageNum";

    @ModelAction(name = ACTION_NAME_LIST,
            icon = "list", forwardToPage = "list",
            nameI18n = {"列表", "en:List"},
            infoI18n = {"展示该类型的所有组件数据或界面。", "en:Show all component data or interfaces of this type."})
    default void list(Request request, Response response) throws Exception {
        String modelName = request.getModelName();
        int totalSize = getDataStore().getTotalSize(modelName);
        response.setTotalSize(totalSize);

        int pageSize = pageSize();
        response.setPageSize(pageSize);

        int pageNum = 1;
        try {
            pageNum = Integer.parseInt(request.getParameter(PARAMETER_PAGE_NUM));
        } catch (NumberFormatException ignored) {
        }
        response.setPageNum(pageNum);

        String[] dataIdInPage = getDataStore().getDataIdInPage(modelName, pageSize, pageNum).toArray(new String[0]);
        ModelManager manager = getAppContext().getModelManager();
        String[] fieldNamesToList = Arrays.stream(manager.getFieldNames(modelName)).filter(s -> manager.getModelField(modelName, s).showToList()).toArray(String[]::new);
        List<Map<String, String>> result = getDataStore().getDataFieldByIds(modelName, dataIdInPage, fieldNamesToList);
        for (Map<String, String> data : result) {
            response.addData(data);
        }
    }

    default List<String> getAllDataId(String modelName) throws Exception {
        DataStore dataStore = getDataStore();
        return dataStore.getAllDataId(modelName);
    }

    // 定制返回的数据分页大小
    default int pageSize() {
        return 10;
    }
}
