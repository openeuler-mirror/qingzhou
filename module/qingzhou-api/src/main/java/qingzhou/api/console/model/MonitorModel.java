package qingzhou.api.console.model;

import qingzhou.api.console.ConsoleContext;
import qingzhou.api.console.ModelAction;
import qingzhou.api.console.MonitoringField;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface MonitorModel {
    String ACTION_NAME_MONITOR = "monitor";
    String MONITOR_EXT_SEPARATOR = ":";
    String OVERVIEW_DATA_KEY_NAME = "name";
    String OVERVIEW_DATA_KEY_MAX = "max";
    String OVERVIEW_DATA_KEY_VALUE = "value";
    String OVERVIEW_DATA_KEY_UNIT = "unit";

    @ModelAction(name = ACTION_NAME_MONITOR,
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
        for (Map.Entry<String, MonitoringField> entry : getConsoleContext().getModelManager().getModelMonitoringFieldMap(request.getModelName()).entrySet()) {
            String fieldName = entry.getKey();
            MonitoringField monitoringField = entry.getValue();
            if (monitoringField.supportGraphicalDynamic()) {
                graphicalDynamicFields.add(fieldName);
            } else {
                String value = p.get(fieldName);
                if (value != null) {
                    if (monitoringField.supportGraphicalDynamic()) {
                        graphicalDynamicFields.add(fieldName);
                    } else if (monitoringField.supportGraphical()) {
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

        response.monitorData().addData(monitorData);
        response.modelData().addData(p);
    }

    ConsoleContext getConsoleContext();

    Map<String, String> monitorData();

    /**
     * Map<String, Object> key:
     * qingzhou.api.console.model.MonitorModel#OVERVIEW_DATA_KEY_NAME
     * qingzhou.api.console.model.MonitorModel#OVERVIEW_DATA_KEY_MAX
     * qingzhou.api.console.model.MonitorModel#OVERVIEW_DATA_KEY_VALUE
     * qingzhou.api.console.model.MonitorModel#OVERVIEW_DATA_KEY_UNIT
     */
    default List<Map<String, Object>> showToOverViewData() {
        return null;
    }
}
