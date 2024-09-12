package qingzhou.console.controller.rest;

import qingzhou.api.Response;
import qingzhou.api.type.Showable;
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
        I18n.addKeyI18n("validation_action", new String[]{"不支持%s操作，未满足条件：%s", "en:The %s operation is not supported, the condition is not met: %s"});
    }

    @Override
    public boolean doFilter(RestContext context) throws Exception {
        RequestImpl request = context.request;
        ModelInfo modelInfo = request.getCachedModelInfo();
        ModelActionInfo actionInfo = modelInfo.getModelActionInfo(request.getAction());
        String condition = actionInfo.getShow();
        if (Utils.isBlank(condition)) return true;

        List<String> checkIds = new ArrayList<>();
        if (context.batchIds != null) {
            checkIds.addAll(Arrays.asList(context.batchIds));
        } else {
            checkIds.add(request.getId());
        }

        for (String id : checkIds) {
            if (Utils.isBlank(id)) continue;

            if (SecurityController.isShow(condition, new FindValue(id, request))) continue;

            String i18n = I18n.getKeyI18n("validation_action", actionInfo.getCode(), actionInfo.getShow());
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
            if (fieldName.equals(request.getCachedModelInfo().getIdFieldName())) return id;

            String parameter = request.getParameter(fieldName); // 优先使用 客户端参数
            if (parameter == null) {
                if (originData == null) {
                    RequestImpl tmp = new RequestImpl();
                    tmp.setAppName(request.getApp());
                    tmp.setModelName(request.getModel());
                    tmp.setActionName(Showable.ACTION_SHOW);
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
