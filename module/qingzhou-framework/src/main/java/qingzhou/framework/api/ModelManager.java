package qingzhou.framework.api;

import java.util.Map;

public interface ModelManager {
    String[] getAllModelNames();

    Model[] getAllModels();

    Model getModel(String modelName);

    Class<?> getModelClass(String modelName);

    ModelBase getModelInstance(String modelName);

    ModelAction[] getModelActions(String modelName);

    ModelAction getModelAction(String modelName, String actionName);

    Map<String, String> getModelDefaultProperties(String modelName);

    String[] getAllFieldNames(String modelName);

    String[] getAllFieldNames(Class<?> modelClass);

    String getModelName(Class<?> modelClass);

    String[] getShowField(String modelName, String actionName);

    ModelField getModelField(String modelName, String fieldName);

    Map<String, ModelField> getModelFieldMap(String modelName);

    Map<String, MonitoringField> getModelMonitoringFieldMap(String modelName);

    Map<String, Map<String, ModelField>> getGroupedModelFieldMap(String modelName);

    String getFieldName(String modelName, int fieldIndex);

    String[] getAllGroupNames(String modelName);

    String[] getFieldNamesByGroup(String modelName, String fieldGroup);
}
