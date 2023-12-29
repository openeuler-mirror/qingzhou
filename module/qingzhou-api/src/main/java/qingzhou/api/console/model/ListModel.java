package qingzhou.api.console.model;

import qingzhou.api.console.DataStore;
import qingzhou.api.console.ModelAction;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;

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
        response.setTotalSize(getTotalSize(request));
        response.setPageSize(pageSize());
        response.setPageNum(pageNum(request));
        int start = (response.getPageNum() - 1) * response.getPageSize();
        if (response.getTotalSize() < start) {
            return;
        }
        List<Map<String, String>> results = listInternal(request, start, response.getPageSize());
        for (Map<String, String> result : results) {
            response.addData(result);
        }
    }


    default List<Map<String, String>> listInternal(Request request, int start, int size) throws Exception {
        return getDataStore().listByPage(request.getModelName(), start, size, FIELD_NAME_ID, true);
    }

    default int getTotalSize(Request request) throws Exception {
        return getDataStore().getTotalSize(request.getModelName());
    }

    default List<String> getAllDataId(String modelName) throws Exception {
        DataStore dataStore = getDataStore();
        return dataStore.getAllDataId(modelName);
    }

    default int pageSize() {
        return 10;
    }

    default int pageNum(Request request) {
        int pageNum = 1;
        String pageNumParameter = request.getParameter(PARAMETER_PAGE_NUM);
        if (pageNumParameter != null) {
            try {
                pageNum = Integer.parseInt(pageNumParameter);
            } catch (NumberFormatException ignored) {
            }
        }
        return pageNum;
    }
}
