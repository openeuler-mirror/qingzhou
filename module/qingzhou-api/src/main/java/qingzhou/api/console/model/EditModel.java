package qingzhou.api.console.model;

import qingzhou.api.console.ConsoleContext;
import qingzhou.api.console.DataStore;
import qingzhou.api.console.ModelAction;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;

import java.util.HashMap;
import java.util.Map;

public interface EditModel extends ShowModel {
    String ACTION_NAME_EDIT = "edit";
    String ACTION_NAME_UPDATE = "update";

    @ModelAction(name = ACTION_NAME_EDIT,
            icon = "edit", forwardToPage = "form",
            nameI18n = {"编辑", "en:Edit"},
            infoI18n = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
    default void edit(Request request, Response response) throws Exception {
        show(request, response);
    }

    @ModelAction(name = ACTION_NAME_UPDATE,
            icon = "save",
            nameI18n = {"更新", "en:Update"},
            infoI18n = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    default void update(Request request) throws Exception {
        DataStore dataStore = getDataStore();
        Map<String, String> properties = prepareParameters(request);
        dataStore.updateSpecifiedData(request.getModelName(), request.getId(), properties);
    }

    default Map<String, String> prepareParameters(Request request) {
        Map<String, String> properties = new HashMap<>();
        ConsoleContext consoleContext = getConsoleContext();
        String[] fieldNames = consoleContext.getModelManager().getAllFieldNames(request.getModelName());
        for (String fieldName : fieldNames) {
            String value = request.getParameter(fieldName);
            if (value != null) {
                properties.put(fieldName, value);
            }
        }
        return properties;
    }
}
