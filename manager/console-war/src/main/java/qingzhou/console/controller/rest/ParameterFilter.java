package qingzhou.console.controller.rest;

import qingzhou.api.InputType;
import qingzhou.api.type.Add;
import qingzhou.api.type.Update;
import qingzhou.console.SecurityController;
import qingzhou.console.controller.SystemController;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.registry.ModelFieldInfo;
import qingzhou.registry.ModelInfo;

import java.text.SimpleDateFormat;
import java.util.*;

public class ParameterFilter implements Filter<RestContext> {
    @Override
    public boolean doFilter(RestContext context) throws Exception {
        RequestImpl request = context.request;

        // 剔除前端不要的表单参数
        remove(request);
        // trim 有效的值
        trim(request);
        // 密码传输解密
        password(request);
        // 日期组件格式解码
        datetime(request);
        // 设置批量处理的标记
        batchId(request);
        // 设置 ActionType.sub_menu 相关参数
        subMenu(request);

        return true;
    }

    private void remove(RequestImpl request) {
        List<String> toRemove = new ArrayList<>();
        boolean isAddAction = Add.ACTION_ADD.equals(request.getAction());
        boolean isUpdateAction = Update.ACTION_UPDATE.equals(request.getAction());

        ModelInfo modelInfo = request.getCachedModelInfo();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            ModelFieldInfo fieldInfo = modelInfo.getModelFieldInfo(name);
            if (fieldInfo == null) continue;

            if (isAddAction) {
                if (!fieldInfo.isCreate()) {
                    toRemove.add(name);
                    continue;
                }
            }

            if (isUpdateAction) {
                if (!fieldInfo.isEdit()) {
                    toRemove.add(name);
                    continue;
                }

                // readonly 要从后端数据校验，避免通过 rest api 绕过前端进入数据写入
                if (fieldInfo.isPlainText()) {
                    toRemove.add(name);
                    continue;
                }
            }

            String display = fieldInfo.getDisplay();
            if (Utils.notBlank(display)) {
                if (!SecurityController.checkRule(display, request::getParameter)) {
                    toRemove.add(name);
                }
            }
        }

        String idField = modelInfo.getIdField();
        for (String f : toRemove) {
            if (f.equals(idField)) continue;
            request.getParameters().remove(f);
        }
    }

    private void trim(RequestImpl request) {
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            String val = request.getParameter(name);
            if (val != null) {
                request.getParameters().put(name, val.trim());
            }
        }
    }

    private void datetime(RequestImpl request) {
        ModelInfo modelInfo = request.getCachedModelInfo();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String fieldName = parameterNames.nextElement();
            ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);
            if (modelField == null) continue;

            if (modelField.getInputType() == InputType.datetime) {
                try {
                    String val = request.getParameter(fieldName);
                    if (Utils.notBlank(val)) {
                        long time = new SimpleDateFormat(DeployerConstants.DATETIME_FORMAT).parse(val).getTime();
                        request.getParameters().put(fieldName, String.valueOf(time));
                    }
                } catch (Exception ignored) {
                }
            }

            if (modelField.getInputType() == InputType.range_datetime) {
                try {
                    String val = request.getParameter(fieldName);
                    if (Utils.notBlank(val)) {
                        String sp = modelField.getSeparator();
                        String[] values = val.split(sp);
                        SimpleDateFormat format = new SimpleDateFormat(DeployerConstants.DATETIME_FORMAT);
                        Date v1 = format.parse(values[0]);
                        Date v2 = format.parse(values[1]);
                        request.getParameters().put(fieldName, String.join(sp, v1.getTime() + sp + v2.getTime()));
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void password(RequestImpl request) {
        ModelInfo modelInfo = request.getCachedModelInfo();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String fieldName = parameterNames.nextElement();
            ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);
            if (modelField == null) continue;
            if (modelField.getInputType() == InputType.password) {
                try {
                    String val = request.getParameter(fieldName);
                    String result = SystemController.decryptWithConsolePrivateKey(val, false);
                    if (result != null) { // 可能是空串
                        request.getParameters().put(fieldName, result);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void batchId(RequestImpl request) {
        ModelInfo modelInfo = request.getCachedModelInfo();

        String[] batchActions = modelInfo.getBatchActions();
        if (batchActions.length == 0) return;
        if (Arrays.stream(batchActions).noneMatch(s -> s.equals(request.getAction()))) return;

        String idField = modelInfo.getIdField();
        String id = request.getParameter(idField);
        ModelFieldInfo idFieldInfo = modelInfo.getModelFieldInfo(idField);
        if (Utils.isBlank(id) || !id.contains(idFieldInfo.getSeparator())) return;

        Set<String> batchIds = new HashSet<>();
        String[] splitIds = id.split(idFieldInfo.getSeparator());
        for (String splitId : splitIds) {
            if (Utils.notBlank(splitId)) {
                batchIds.add(splitId);
            }
        }
        request.setBatchId(batchIds.toArray(new String[0]));
        request.getParameters().remove(idField);
    }

    private void subMenu(RequestImpl request) {
        List<String> toMove = new ArrayList<>();

        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            if (name.startsWith(DeployerConstants.SUB_MENU_PARAMETER_FLAG)) {
                toMove.add(name);
            }
        }

        for (String subMenuParam : toMove) {
            String value = request.getParameters().remove(subMenuParam);
            String key = subMenuParam.substring(DeployerConstants.SUB_MENU_PARAMETER_FLAG.length());
            request.parametersForSubMenu().put(key, value);
        }
    }
}