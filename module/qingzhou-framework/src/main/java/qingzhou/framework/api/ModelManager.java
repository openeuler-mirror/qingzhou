package qingzhou.framework.api;

import java.util.Map;

public interface ModelManager {
    String[] getModelNames();

    String getModelName(Class<?> modelClass);

    Class<?> getModelClass(String modelName);

    boolean isModelType(String modelName, Class<?> modelType);

    ModelBase getModelInstance(String modelName);

    Map<String, String> getModelDefaultProperties(String modelName);

    Model getModel(String modelName);

    String[] getActionNames(String modelName);

    String[] getActionNamesToFormBottom(String modelName);

    String[] getActionNamesSupportBatch(String modelName);

    String[] getActionNamesToList(String modelName);

    String[] getActionNamesToListHead(String modelName);

    ModelAction getModelAction(String modelName, String actionName);

    String[] getFieldNames(String modelName);

    String[] getGroupNames(String modelName);

    String[] getFieldNamesByGroup(String modelName, String groupName);

    Group getGroup(String modelName, String groupName);

    String getFieldName(String modelName, int fieldIndex);

    ModelField getModelField(String modelName, String fieldName);

    Options getOptions(String modelName, String fieldName);

    Map<String, MonitorField> getModelMonitoringFieldMap(String modelName);// todo: MonitorField 合并如 ModelField？
}
