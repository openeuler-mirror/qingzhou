package qingzhou.api;

import qingzhou.serialization.ModelActionData;
import qingzhou.serialization.ModelData;
import qingzhou.serialization.ModelFieldData;

import java.util.Map;

public interface ModelManager {
    String[] getModelNames();

    Map<String, String> getModelDefaultProperties(String modelName);

    ModelData getModel(String modelName);

    String[] getActionNames(String modelName);

    String[] getActionNamesSupportBatch(String modelName);

    ModelActionData getModelAction(String modelName, String actionName);

    String[] getFieldNames(String modelName);

    String[] getGroupNames(String modelName);

    String[] getFieldNamesByGroup(String modelName, String groupName);

    Group getGroup(String modelName, String groupName);

    ModelFieldData getModelField(String modelName, String fieldName);

    Map<String, ModelFieldData> getMonitorFieldMap(String modelName);

    Options getOptions(Request request, String modelName, String fieldName);
}
