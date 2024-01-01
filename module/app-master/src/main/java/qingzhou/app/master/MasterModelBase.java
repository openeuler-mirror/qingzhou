package qingzhou.app.master;

import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelManager;
import qingzhou.framework.util.ObjectUtil;

import java.util.HashMap;
import java.util.Map;

public class MasterModelBase extends ModelBase {
    // todo
    protected <T> Map<String, String> mapper(T data) {
        ModelManager manager = getAppContext().getModelManager();
        String modelName = manager.getModelName(data.getClass());
        String[] classFields = manager.getFieldNames(modelName);
        Map<String, String> map = new HashMap<>();
        for (String field : classFields) {
            try {
                String objectValue = String.valueOf(ObjectUtil.getObjectValue(data, field));
                map.put(field, objectValue);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return map;
    }
}
