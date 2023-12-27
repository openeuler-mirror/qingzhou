package qingzhou.api.console.model;

import qingzhou.api.console.ConsoleContext;
import qingzhou.api.console.DataStore;
import qingzhou.api.console.ModelAction;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;

import java.util.Map;

public interface AddModel extends EditModel, DeleteModel {
    String ACTION_NAME_CREATE = "create";
    String ACTION_NAME_ADD = "add";

    @ModelAction(name = ACTION_NAME_CREATE,
            icon = "plus-sign", forwardToPage = "form",
            nameI18n = {"创建", "en:Create"},
            infoI18n = {"获得创建该组件的默认数据或界面。", "en:Get the default data or interface for creating this component."})
    default void create(Request request, Response response) throws Exception {
        ConsoleContext consoleContext = getConsoleContext();
        Map<String, String> properties = consoleContext.getModelManager().getModelDefaultProperties(request.getModelName());
        response.modelData().addData(properties);
    }

    @ModelAction(name = ACTION_NAME_ADD,
            icon = "save",
            nameI18n = {"添加", "en:Add"},
            infoI18n = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    default void add(Request request) throws Exception {
        Map<String, String> properties = prepareParameters(request);
        String id = properties.get(FIELD_NAME_ID);
        DataStore dataStore = getDataStore();
        dataStore.addData(request.getModelName(), id, properties);
    }
}
