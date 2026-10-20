package qingzhou.registry.web;

import java.util.Map;

public interface RequestParameters {
    String INSTANCE_ID = "instanceId";
    String APP_CODE = "appCode";
    String MODEL_CODE = "modelCode";
    String ACTION_CODE = "actionCode";
    String DATA_ID = "dataId";

    String getParameter(String name);

    /**
     * 从工具入参构造请求参数：值统一转字符串，缺失返回 null
     */
    static RequestParameters of(Map<String, Object> toolArgs) {
        return name -> {
            if (toolArgs == null) return null;
            Object val = toolArgs.get(name);
            return val != null ? String.valueOf(val) : null;
        };
    }
}
