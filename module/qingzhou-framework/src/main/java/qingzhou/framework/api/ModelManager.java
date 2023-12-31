package qingzhou.framework.api;

import java.util.Map;

public interface ModelManager {
    String[] getModelNames();

    String getModelName(Class<?> modelClass);

    Class<?> getModelClass(String modelName);

    ModelBase getModelInstance(String modelName);

    Map<String, String> getModelDefaultProperties(String modelName);

    Model getModel(String modelName);

    String[] getActionNames(String modelName);

    String[] getActionNamesShowToFormBottom(String modelName);

    String[] getActionNamesSupportBatch(String modelName);

    String[] getActionNamesShowToList(String modelName);

    String[] getActionNamesShowToListHead(String modelName);

    ModelAction getModelAction(String modelName, String actionName);

    String[] getFieldNames(String modelName);

    String[] getGroupNames(String modelName);

    String[] getFieldNamesByGroup(String modelName, String groupName);

    Group getGroup(String modelName, String groupName);

    String getFieldName(String modelName, int fieldIndex);

    ModelField getModelField(String modelName, String fieldName);

    Map<String, ModelField> getMonitorFieldMap(String modelName);

    Options getOptions(String modelName, String fieldName);
}
