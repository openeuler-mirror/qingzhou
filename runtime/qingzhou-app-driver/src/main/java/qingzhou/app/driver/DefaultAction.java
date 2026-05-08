package qingzhou.app.driver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import qingzhou.api.FieldType;
import qingzhou.api.ModelAction;
import qingzhou.api.Request;
import qingzhou.api.type.*;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelField;

/**
 * 被 AppStubLocalImpl#invokeAction 方法反射调用
 */
public class DefaultAction {
    private DefaultAction() {
    }

    @ModelAction(
            code = Add.ACTION_CODE_CREATE, icon = "Plus", order = 1,
            name = {"新增", "en:Create"}, list_head = true,
            info = {"新增一条记录。", "en:Create a new record."})
    public static void create(Add add, Request request) {
    }

    @ModelAction(
            code = Add.ACTION_CODE_ADD, icon = "Check", order = 1,
            name = {"保存", "en:Add"}, add = true,
            info = {"保存新建的记录。", "en:Save the newly created record."})
    public static void add(Add add, Request request) throws Exception {
        Map<String, String> saveData = toSaveData(request, modelField -> modelField.add);
        add.add(request, saveData);
    }

    @ModelAction(
            code = Update.ACTION_CODE_EDIT, icon = "Edit", order = 1,
            name = {"编辑", "en:Edit"}, list = true,
            info = {"编辑本条记录。", "en:Edit this record."})
    public static void edit(Update update, Request request) {
    }

    @ModelAction(
            code = Update.ACTION_CODE_UPDATE, icon = "Check", order = 1,
            name = {"更新", "en:Update"}, update = true,
            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public static void update(Update update, Request request) throws Exception {
        Map<String, String> saveData = toSaveData(request, modelField -> modelField.update);

        String[] idFields = selectFormFields(request, field -> field.id);
        if (idFields.length > 0) {
            saveData.remove(idFields[0]);
        }
        update.update(request, saveData);
    }

    @ModelAction(
            code = Show.ACTION_CODE_SHOW, icon = "Document",
            name = {"查看", "en:Show"}, list = true, show = true,
            info = {"查看该组件的相关信息。", "en:View the information of this model."})
    public static void show(Show show, Request request) throws Exception {
        Map<String, String> showData = show.show(request);
        ResponseImpl response = (ResponseImpl) request.getResponse();
        if (response.getData() == null && showData != null) {
            String[] showFields = selectFormFields(request, modelField -> modelField.show);
            response.data(filterMapData(showData, showFields));
        }
    }

    @ModelAction(
            code = Monitor.ACTION_CODE_MONITOR, icon = "TrendCharts",
            name = {"监视", "en:Monitor"},
            info = {"查看该组件的相关信息。", "en:View the information of this model."})
    public static void monitor(Monitor monitor, Request request) throws Exception {
        Map<String, String> monitorData = monitor.monitor(request);
        ResponseImpl response = (ResponseImpl) request.getResponse();
        if (response.getData() == null && monitorData != null) {
            String[] monitorFields = selectMonitoringFields(request, modelField -> true);
            response.data(filterMapData(monitorData, monitorFields));
        }
    }

    @ModelAction(
            code = qingzhou.api.type.List.ACTION_CODE_LIST, icon = "List",
            name = {"列表", "en:List"},
            info = {"展示该类型的所有组件数据或界面。", "en:Show all component data or interfaces of this type."})
    public static void list(qingzhou.api.type.List list, Request request) throws Exception {
        Map<String, String> query = new HashMap<>();
        for (String search : selectFormFields(request, modelField -> modelField.search)) {
            String parameter = request.getParameter(search);
            if (parameter != null && !parameter.trim().isEmpty()) {
                query.put(search, parameter.trim());
            }
        }

        String[] showFields = selectFormFields(request, modelField -> modelField.list);
        int pageNum = parsePageParam(request.getParameter("pageNum"), 1);
        int pageSize = Math.min(parsePageParam(request.getParameter("pageSize"), 10), 100);
        List<String[]> listData = list.list(request, pageNum, pageSize, query, showFields);
        ResponseImpl response = (ResponseImpl) request.getResponse();
        if (response.getData() == null && listData != null) {
            List<Map<String, String>> listResult = new ArrayList<>();
            listData.forEach(data -> {
                Map<String, String> dataMap = new HashMap<>(); // 没必要在此处保证顺序了，因为远程调用 json 序列化还是会丢掉顺序
                for (int i = 0; i < showFields.length; i++) {
                    dataMap.put(showFields[i], data[i]);
                }
                listResult.add(dataMap);
            });
            response.data(listResult);
        }
    }

    @ModelAction(
            code = Delete.ACTION_CODE_DELETE, icon = "Delete", order = 2,
            name = {"删除", "en:Delete"}, list = true, batch = true,
            info = {"删除该模块。", "en:Delete this module."})
    public static void delete(Delete delete, Request request) throws Exception {
        String id = request.getId();
        if (id != null && !id.isEmpty()) {
            delete.delete(id);
        }

        String ids = request.getParameter("ids");
        if (ids != null && !ids.isEmpty()) {
            String[] idArray = ids.split(",");
            for (String itemId : idArray) {
                if (itemId != null && !itemId.isEmpty()) {
                    delete.delete(itemId);
                }
            }
        }
    }

    private static Map<String, String> filterMapData(Map<String, String> data, String[] useFields) {
        Map<String, String> filteredMap = new HashMap<>();
        for (String useField : useFields) {
            String val = data.get(useField);
            if (val != null) {
                filteredMap.put(useField, val);
            }
        }
        return filteredMap;
    }

    private static Map<String, String> toSaveData(Request request, Predicate<ModelField> predicate) {
        Map<String, String> data = new HashMap<>();
        for (String field : selectFormFields(request, predicate)) {
            String parameter = request.getParameter(field);
            if (parameter != null) {
                data.put(field, parameter);
            }
        }
        return data;
    }

    private static int parsePageParam(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int result = Integer.parseInt(value.trim());
            return result > 0 ? result : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String[] selectFormFields(Request request, Predicate<ModelField> predicate) {
        RequestImpl requestImpl = (RequestImpl) request;
        Model currentModel = requestImpl.getCurrentModel();
        return currentModel.fields.stream().filter(modelField -> modelField.field_type == FieldType.FORM).filter(predicate).map(mf -> mf.code).toArray(String[]::new);
    }

    private static String[] selectMonitoringFields(Request request, Predicate<ModelField> predicate) {
        RequestImpl requestImpl = (RequestImpl) request;
        Model currentModel = requestImpl.getCurrentModel();
        return currentModel.fields.stream().filter(modelField -> modelField.field_type == FieldType.MONITORING).filter(predicate).map(mf -> mf.code).toArray(String[]::new);
    }
}
