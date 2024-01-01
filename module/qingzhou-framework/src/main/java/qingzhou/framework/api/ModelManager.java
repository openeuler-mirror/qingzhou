package qingzhou.framework.api;

import java.util.Map;

public interface ModelManager {
    String[] getAllModelNames();

    String getModelName(Class<?> modelClass);

    ModelBase getModelInstance(String modelName);

    Map<String, String> getModelDefaultProperties(String modelName);

    Model[] getAllModels();

    Model getModel(String modelName);

    Class<?> getModelClass(String modelName);

    ModelAction[] getModelActions(String modelName);

    ModelAction getModelAction(String modelName, String actionName);

    String[] getAllFieldNames(String modelName);

    String[] getAllFieldNames(Class<?> modelClass);

    String[] getShowField(String modelName, String actionName);

    ModelField getModelField(String modelName, String fieldName);

    Map<String, ModelField> getModelFieldMap(String modelName);

    Map<String, MonitoringField> getModelMonitoringFieldMap(String modelName);

    Map<String, Map<String, ModelField>> getGroupedModelFieldMap(String modelName);

    String getFieldName(String modelName, int fieldIndex);

    String[] getAllGroupNames(String modelName);

    String[] getFieldNamesByGroup(String modelName, String fieldGroup);
}
