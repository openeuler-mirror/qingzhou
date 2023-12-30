package qingzhou.master;

import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.util.ModelUtil;
import qingzhou.framework.util.ObjectUtil;

import java.util.HashMap;
import java.util.Map;

public class MasterModelBase extends ModelBase {
    static {
        ConsoleContext consoleContext = ModelUtil.getMasterConsoleContext();
        if (consoleContext != null) {
            consoleContext.addI18N("user.not.permission", new String[]{"当前登录用户没有执行此操作的权限。", "en:The currently login user does not have permission to do this"});
        }
    }

    @Override
    public ConsoleContext getConsoleContext() {
        return ModelUtil.getMasterConsoleContext();
    }

    protected <T> Map<String, String> mapper(T data) {
        Class<?> dataClassType = data.getClass();
        String[] classFields = ModelUtil.getMasterAppContext().getModelManager().getAllFieldNames(dataClassType);
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
