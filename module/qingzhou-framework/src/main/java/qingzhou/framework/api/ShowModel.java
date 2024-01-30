package qingzhou.framework.api;

import java.util.Map;

public interface ShowModel {
    String ACTION_NAME_SHOW = "show";

    @ModelAction(name = ACTION_NAME_SHOW,
            icon = "info-sign", forwardToPage = "show",
            nameI18n = {"查看", "en:Show"},
            infoI18n = {"查看该组件的相关信息。", "en:View the information of this model."})
    default void show(Request request, Response response) throws Exception {
        response.addData(showInternal(request));
    }

    default Map<String, String> showInternal(Request request) throws Exception {
        DataStore dataStore = getDataStore();
        return dataStore.getDataById(request.getModelName(), request.getId());
    }


    default DataStore getDataStore() {
        return getAppContext().getDataStore();
    }

    AppContext getAppContext();
}
