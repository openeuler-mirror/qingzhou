package qingzhou.app.master;

import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.util.ObjectUtil;
import qingzhou.framework.util.ServerUtil;

import java.util.HashMap;
import java.util.Map;

public class MasterModelBase extends ModelBase {
    @Override
    public AppContext getAppContext() {
        return ServerUtil.getMasterAppContext();
    }

    protected <T> Map<String, String> mapper(T data) {
        Class<?> dataClassType = data.getClass();
        String[] classFields = ServerUtil.getMasterAppContext().getModelManager().getAllFieldNames(dataClassType);
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
