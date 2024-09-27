package qingzhou.console.controller.rest;

import qingzhou.api.Response;
import qingzhou.api.type.Add;
import qingzhou.api.type.List;
import qingzhou.api.type.Show;
import qingzhou.console.SecurityController;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.RequestImpl;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.registry.ModelActionInfo;
import qingzhou.registry.ModelInfo;

import java.util.*;

public class ActionFilter implements Filter<RestContext> {
    static {
        I18n.addKeyI18n("action_not_show", new String[]{"不支持%s操作，未满足条件：%s", "en:The %s operation is not supported, the condition is not met: %s"});
        I18n.addKeyI18n("action_not_exist", new String[]{"不存在", "en:Does not exist"});
    }

    @Override
    public boolean doFilter(RestContext context) throws Exception {
        if (!show(context)) return false;
        return exists(context);
    }

    private boolean exists(RestContext context) {
        RequestImpl request = context.request;

        if (request.getAction().equals(Add.ACTION_ADD) // 添加是带有id的，但不用校验
                || request.getAction().equals(List.ACTION_LIST)) {
            return true;
        }

        String id = request.getId();
        if (Utils.isBlank(id)) return true; // 非 rest id 操作，无需校验

        ModelInfo modelInfo = request.getCachedModelInfo();
        if (modelInfo.getModelActionInfo(List.ACTION_CONTAINS) == null) return true; // 不是 list 类型 model,无需 校验

        RequestImpl tmp = new RequestImpl();
        tmp.setAppName(request.getApp());
        tmp.setModelName(request.getModel());
        tmp.setActionName(List.ACTION_CONTAINS);
        tmp.setId(id);
        Response tmpResp = SystemController.getService(ActionInvoker.class).invokeSingle(tmp);
        boolean success = tmpResp.isSuccess();
        if (!success) {
            String i18n = I18n.getKeyI18n("action_not_exist");
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg(i18n);
        }
        return success;
    }

    private boolean show(RestContext context) {
        RequestImpl request = context.request;
        ModelInfo modelInfo = request.getCachedModelInfo();
        ModelActionInfo actionInfo = modelInfo.getModelActionInfo(request.getAction());
        String condition = actionInfo.getShow();
        if (Utils.isBlank(condition)) return true;

        java.util.List<String> checkIds = new ArrayList<>();
        if (context.batchIds != null) {
            checkIds.addAll(Arrays.asList(context.batchIds));
        } else {
            checkIds.add(request.getId());
        }

        for (String id : checkIds) {
            if (Utils.isBlank(id)) continue;

            if (SecurityController.checkRule(condition, new FindValue(id, request), true)) continue;

            String i18n = I18n.getKeyI18n("action_not_show", actionInfo.getCode(), actionInfo.getShow());
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg(i18n);
            return false;
        }

        return true;
    }

    private static class FindValue implements SecurityController.FieldValueRetriever {
        private final String id;
        private final RequestImpl request;
        private Map<String, String> originData;

        private FindValue(String id, RequestImpl request) {
            this.id = id;
            this.request = request;
        }

        @Override
        public String getFieldValue(String fieldName) {
            if (fieldName.equals(request.getCachedModelInfo().getIdField())) return id;

            String parameter = request.getParameter(fieldName); // 优先使用 客户端参数
            if (parameter == null) {
                if (originData == null) {
                    RequestImpl tmp = new RequestImpl();
                    tmp.setAppName(request.getApp());
                    tmp.setModelName(request.getModel());
                    tmp.setActionName(Show.ACTION_SHOW);
                    tmp.setId(id);
                    Response response = SystemController.getService(ActionInvoker.class).invokeSingle(tmp);
                    if (!response.getDataList().isEmpty()) {
                        originData = response.getDataList().get(0);
                    } else {
                        originData = new HashMap<>();
                    }
                }

                parameter = originData.get(fieldName);
            }

            return parameter;
        }
    }
}
