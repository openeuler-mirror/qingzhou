package qingzhou.console;

import qingzhou.api.Request;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.deployer.ResponseImpl;
import qingzhou.registry.AppInfo;
import qingzhou.registry.ModelInfo;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConsoleActionInvoker { // todo
    static {
        I18n.addKeyI18n("batch.ops.fail", new String[]{"%s%s成功%s个，失败%s个，失败详情：", "en:%s%s success %s, failure %s, failure details:"});// 一些 filter 需要 i18n，如 LoginFreeFilter 调用了Helper.convertCommonMsg(msg)，此时 RestController 等类可能都还没有初始化（例如 Rest 直连登录），会导致 i18n 信息丢失，因此放到这里
    }

    public void invokeAction(RequestImpl request) {
        if (isBatchAction(request)) {
            ResponseImpl response = invokeBatch(request);
            request.setResponse(response);
        } else {
            invoke(request);
        }
    }

    private boolean isBatchAction(Request request) {
        AppInfo appInfo = SystemController.getAppInfo(SystemController.getAppName(request));
        if (appInfo == null) return false;
        ModelInfo modelInfo = appInfo.getModelInfo(request.getModel());
        String ids = request.getParameter(modelInfo.getIdFieldName());
        if (ids == null) return false;
        String[] actionNamesSupportBatch = modelInfo.getBatchActionNames();
        for (String batch : actionNamesSupportBatch) {
            if (batch.equals(request.getAction())) return true;
        }
        return false;
    }

    private ResponseImpl invokeBatch(RequestImpl request) {
        ResponseImpl response = new ResponseImpl();
        int suc = 0;
        int fail = 0;
        StringBuilder errbuilder = new StringBuilder();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        AppInfo appInfo = SystemController.getAppInfo(SystemController.getAppName(request));
        ModelInfo modelInfo = appInfo.getModelInfo(request.getModel());
        String oid = request.getParameter(modelInfo.getIdFieldName());
        for (String id : oid.split(DeployerConstants.DEFAULT_DATA_SEPARATOR)) {
            if (!id.isEmpty()) {
                id = RESTController.decodeId(id);
                request.setId(id);
                response = invoke(request);
                if (response.isSuccess()) {
                    suc++;
                } else {
                    String actionContextMsg = response.getMsg();
                    if (result.containsKey(actionContextMsg)) {
                        errbuilder.append(result.get(actionContextMsg));
                        errbuilder.append(",");
                        errbuilder.append(id);
                        result.put(actionContextMsg, errbuilder.toString());
                        errbuilder.setLength(0);
                    } else {
                        result.put(actionContextMsg, id);
                    }
                    fail++;
                }
            }
        }
        request.setId(oid);
        String appName = SystemController.getAppName(request);
        String model = I18n.getModelI18n(appName, "model." + request.getModel());
        String action = I18n.getModelI18n(appName, "model.action." + request.getModel() + "." + request.getAction());
        if (result.isEmpty()) {
            String resultMsg = I18n.getKeyI18n("batch.ops.success", model, action, suc);
            response.setMsg(resultMsg);
        } else {
            response.setSuccess(suc > 0);
            errbuilder.append(I18n.getKeyI18n("batch.ops.fail", model, action, suc, fail));
            errbuilder.append("<br/>");
            for (Map.Entry<String, String> entry : result.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                errbuilder.append(value).append("= {").append(key).append("};");
                errbuilder.append("<br/>");
            }
            response.setMsg(errbuilder.toString());
        }
        return response;
    }

    private ResponseImpl invoke(RequestImpl request) {
        // todo SystemController.getService(ActionInvoker.class).invokeAuto(request);
        return null;
    }
}
