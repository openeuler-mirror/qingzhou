package qingzhou.framework.api;

import java.util.Map;

public interface AddModel extends EditModel, DeleteModel {
    String ACTION_NAME_CREATE = "create";
    String ACTION_NAME_ADD = "add";

    @ModelAction(name = ACTION_NAME_CREATE,
            showToListHead = true,
            icon = "plus-sign", forwardToPage = "form",
            nameI18n = {"创建", "en:Create"},
            infoI18n = {"获得创建该组件的默认数据或界面。", "en:Get the default data or interface for creating this component."})
    default void create(Request request, Response response) throws Exception {
        Map<String, String> properties = getAppContext().getConsoleContext().getModelManager().getModelDefaultProperties(request.getModelName());
        response.addData(properties);
    }

    @ModelAction(name = ACTION_NAME_ADD,
            icon = "save",
            showToFormBottom = true,
            nameI18n = {"添加", "en:Add"},
            infoI18n = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    default void add(Request request, Response response) throws Exception {
        Map<String, String> properties = prepareParameters(request);
        String id = properties.get(FIELD_NAME_ID);
        DataStore dataStore = getDataStore();
        dataStore.addData(request.getModelName(), id, properties);
    }
}
