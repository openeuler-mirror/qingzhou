package qingzhou.framework.api;

public interface DeleteModel extends ListModel {
    String ACTION_NAME_DELETE = "delete";

    @ModelAction(
            name = ACTION_NAME_DELETE,
            showToList = true, supportBatch = true,
            icon = "trash",
            nameI18n = {"删除", "en:Delete"},
            infoI18n = {"删除这个组件，该组件引用的其它组件不会被删除。注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this component, other components referenced by this component will not be deleted. Note: Please operate with caution, it cannot be recovered after deletion."})
    default void delete(Request request, Response response) throws Exception {
        DataStore dataStore = getDataStore();
        dataStore.deleteDataById(request.getModelName(), request.getId());
    }
}
