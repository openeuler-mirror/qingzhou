package qingzhou.console.controller.rest;

import qingzhou.api.InputType;
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
import java.util.stream.Collectors;

public class ParameterFilter implements Filter<RestContext> {
    @Override
    public boolean doFilter(RestContext context) throws Exception {
        RequestImpl request = context.request;

        // 先分离出表单参数
        separateParameters(request);
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

        return true;
    }

    private void remove(RequestImpl request) {
        List<String> toRemove = new ArrayList<>();
        boolean isUpdateAction = Update.ACTION_UPDATE.equals(request.getAction());

        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            ModelFieldInfo fieldInfo = request.getCachedModelInfo().getModelFieldInfo(name);

            String create = fieldInfo.getEditable();
            if (Utils.notBlank(create)) {
                if (!SecurityController.checkRule(create, request::getParameter)) {
                    toRemove.add(name);
                }
            }

            if (isUpdateAction) {
                // readonly 要从后端数据校验，避免通过 rest api 绕过前端进入数据写入
                if (Utils.notBlank(fieldInfo.getReadOnly())) {
                    boolean isReadOnly = SecurityController.checkRule(fieldInfo.getReadOnly(), new RemoteFieldValueRetriever(request.getId(), request));
                    if (isReadOnly) {
                        toRemove.add(name);
                    }
                }
            }
        }

        toRemove.forEach(request::removeParameter);
    }

    private void trim(RequestImpl request) {
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            String val = request.getParameter(name);
            if (val != null) {
                request.setParameter(name, val.trim());
            }
        }

        Enumeration<String> nonModelParam = request.getNonModelParameterNames();
        while (nonModelParam.hasMoreElements()) {
            String name = nonModelParam.nextElement();
            String val = request.getNonModelParameter(name);
            if (val != null) {
                request.setNonModelParameter(name, val.trim());
            }
        }
    }

    private void separateParameters(RequestImpl request) {
        ModelInfo modelInfo = request.getCachedModelInfo();
        List<String> toRemove = request.getParameters().keySet().stream().filter(param -> Arrays.stream(modelInfo.getFormFieldNames()).noneMatch(s -> s.equals(param))).collect(Collectors.toList());
        toRemove.forEach(p -> {
            String v = request.removeParameter(p);
            request.setNonModelParameter(p, v);
        });
    }

    private void datetime(RequestImpl request) {
        ModelInfo modelInfo = request.getCachedModelInfo();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String fieldName = parameterNames.nextElement();
            ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);
            if (modelField.getInputType() == InputType.datetime) {
                try {
                    String val = request.getParameter(fieldName);
                    if (Utils.notBlank(val)) {
                        long time = new SimpleDateFormat(DeployerConstants.FIELD_DATETIME_FORMAT).parse(val).getTime();
                        request.setParameter(fieldName, String.valueOf(time));
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
            if (modelField.getInputType() == InputType.password) {
                try {
                    String val = request.getParameter(fieldName);
                    String result = SystemController.decryptWithConsolePrivateKey(val, false);
                    if (result != null) { // 可能是空串
                        request.setParameter(fieldName, result);
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
        request.removeParameter(idField);
    }
}