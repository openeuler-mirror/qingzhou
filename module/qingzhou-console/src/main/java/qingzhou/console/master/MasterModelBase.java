package qingzhou.console.master;

import qingzhou.api.console.ConsoleContext;
import qingzhou.api.console.model.ModelBase;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.util.ObjectUtil;

import java.util.HashMap;
import java.util.Map;

public class MasterModelBase extends ModelBase {
    static {
        ConsoleContext consoleContext = ConsoleWarHelper.getMasterAppConsoleContext();
        if (consoleContext != null) {
            consoleContext.addI18N("user.not.permission", new String[]{"当前登录用户没有执行此操作的权限。", "en:The currently login user does not have permission to do this"});
        }
    }

    @Override
    public ConsoleContext getConsoleContext() {
        return ConsoleWarHelper.getMasterAppConsoleContext();
    }

    protected   <T> Map<String, String> mapper(T data) {
        Class<?> dataClassType = data.getClass();
        String[] classFields = getConsoleContext().getModelManager().getAllFieldNames(dataClassType);
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
