package qingzhou.api.console.model;

import qingzhou.api.console.ConsoleContext;
import qingzhou.api.console.DataStore;
import qingzhou.api.console.ModelAction;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;

import java.util.Map;

public interface ShowModel {
    String ACTION_NAME_SHOW = "show";

    @ModelAction(name = ACTION_NAME_SHOW,
            icon = "info-sign", forwardToPage = "info",
            nameI18n = {"查看", "en:Show"},
            infoI18n = {"查看该组件的详细配置信息。", "en:View the detailed configuration information of the component."})
    default void show(Request request, Response response) throws Exception {
        response.addData(showInternal(request));
    }

    default Map<String, String> showInternal(Request request) throws Exception {
        DataStore dataStore = getDataStore();
        return dataStore.getDataById(request.getModelName(), request.getId());
    }

    ConsoleContext getConsoleContext();

    default DataStore getDataStore() {
        return getConsoleContext().getDataStore();
    }
}
