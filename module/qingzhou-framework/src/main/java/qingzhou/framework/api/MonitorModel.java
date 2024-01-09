package qingzhou.framework.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface MonitorModel extends ShowModel {
    String ACTION_NAME_MONITOR = "monitor";
    String MONITOR_EXT_SEPARATOR = ":";
    String OVERVIEW_DATA_KEY_NAME = "name";
    String OVERVIEW_DATA_KEY_MAX = "max";
    String OVERVIEW_DATA_KEY_VALUE = "value";
    String OVERVIEW_DATA_KEY_UNIT = "unit";

    @ModelAction(name = ACTION_NAME_MONITOR,
            showToFormBottom = true, showToList = true,
            icon = "area-chart", forwardToPage = "info",
            nameI18n = {"监视", "en:Monitor"},
            infoI18n = {"获取该组件的运行状态信息，该信息可反映组件的健康情况。",
                    "en:Obtain the operating status information of the component, which can reflect the health of the component."})
    default void monitor(Request request, Response response) throws Exception {
        Map<String, String> p = monitorData();

        if (p.isEmpty()) {
            return;
        }
        List<String> graphicalDynamicFields = new ArrayList<>();
        Map<String, String> monitorData = new HashMap<>();
        for (Map.Entry<String, ModelField> entry : getAppContext().getConsoleContext().getModelManager().getMonitorFieldMap(request.getModelName()).entrySet()) {
            String fieldName = entry.getKey();
            ModelField monitorField = entry.getValue();
            if (monitorField.supportGraphicalDynamic()) {
                graphicalDynamicFields.add(fieldName);
            } else {
                String value = p.get(fieldName);
                if (value != null) {
                    if (monitorField.supportGraphicalDynamic()) {
                        graphicalDynamicFields.add(fieldName);
                    } else if (monitorField.supportGraphical()) {
                        monitorData.put(fieldName, value);
                    }
                }
            }
        }

        // 检查是否有待扩展属性？
        for (String check : graphicalDynamicFields) {
            for (String k : p.keySet()) {
                if (k.startsWith(check + MONITOR_EXT_SEPARATOR)) {
                    monitorData.put(k, p.get(k));
                }
            }
        }

        response.addData(monitorData);
    }

    Map<String, String> monitorData();
}
