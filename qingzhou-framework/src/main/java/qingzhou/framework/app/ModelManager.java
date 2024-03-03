package qingzhou.framework.app;

import qingzhou.api.Group;
import qingzhou.api.Options;
import qingzhou.api.Request;
import qingzhou.framework.app.ModelActionData;
import qingzhou.framework.app.ModelData;
import qingzhou.framework.app.ModelFieldData;

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
