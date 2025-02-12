package qingzhou.console.controller.rest;

import java.util.ArrayList;
import java.util.Arrays;

import qingzhou.api.type.Show;
import qingzhou.console.controller.I18n;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.registry.ModelActionInfo;
import qingzhou.core.registry.ModelInfo;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;

public class ActionFilter implements Filter<RestContext> {
    static {
        I18n.addKeyI18n("action_not_show", new String[]{"不支持 %s %s，未满足条件：%s", "en:%s %s is not supported, condition not met: %s"});
    }

    @Override
    public boolean doFilter(RestContext context) throws Exception {
        return checkDisplay(context);
    }

    private boolean checkDisplay(RestContext context) {
        RequestImpl request = context.request;
        ModelInfo modelInfo = request.getCachedModelInfo();

        // FindValue 中需要 Show.ACTION_SHOW
        if (modelInfo.getModelActionInfo(Show.ACTION_SHOW) == null) return true;

        ModelActionInfo actionInfo = modelInfo.getModelActionInfo(request.getAction());
        String condition = actionInfo.getDisplay();
        if (Utils.isBlank(condition)) return true;

        java.util.List<String> checkIds = new ArrayList<>();
        if (request.getBatchId() != null) {
            checkIds.addAll(Arrays.asList(request.getBatchId()));
        } else {
            checkIds.add(request.getId());
        }

        for (String id : checkIds) {
            if (Utils.isBlank(id)) continue;

            if (SecurityController.checkRule(condition, new RemoteFieldValueRetriever(id, request))) continue;

            String i18n = I18n.getKeyI18n("action_not_show",
                    I18n.getModelI18n(request.getApp(), "model.action." + request.getModel() + "." + request.getAction()),
                    id, condition);
            request.getResponse().setSuccess(false);
            request.getResponse().setMsg(i18n);
            return false;
        }

        return true;
    }
}
