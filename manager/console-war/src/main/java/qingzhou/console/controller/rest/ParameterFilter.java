package qingzhou.console.controller.rest;

import qingzhou.api.FieldType;
import qingzhou.console.controller.SystemController;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.registry.ModelFieldInfo;
import qingzhou.registry.ModelInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ParameterFilter implements Filter<RestContext> {
    @Override
    public boolean doFilter(RestContext context) throws Exception {
        RequestImpl request = context.request;

        trim(request);
        separateParameters(request);
        password(request);
        batchId(request, context);

        return true;
    }

    private void trim(RequestImpl request) {
        for (String fieldName : request.getCachedModelInfo().getFormFieldNames()) {
            String val = request.getParameter(fieldName);
            if (val != null) {
                request.setParameter(fieldName, val.trim());
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

    private void password(RequestImpl request) {
        ModelInfo modelInfo = request.getCachedModelInfo();
        for (String fieldName : modelInfo.getFormFieldNames()) {
            ModelFieldInfo modelField = modelInfo.getModelFieldInfo(fieldName);
            if (modelField.getType().equals(FieldType.password.name())) {
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

    private void batchId(RequestImpl request, RestContext context) {
        ModelInfo modelInfo = request.getCachedModelInfo();
        String idFieldName = modelInfo.getIdFieldName();
        String id = request.getParameter(idFieldName);
        if (id != null && id.contains(DeployerConstants.BATCH_ID_SEPARATOR)) {
            Set<String> batchIds = new HashSet<>();
            String[] splitIds = id.split(DeployerConstants.BATCH_ID_SEPARATOR);
            for (String splitId : splitIds) {
                if (Utils.notBlank(splitId)) {
                    batchIds.add(splitId);
                }
            }
            context.batchIds = batchIds.toArray(new String[0]);
            request.removeParameter(idFieldName);
        }
    }
}