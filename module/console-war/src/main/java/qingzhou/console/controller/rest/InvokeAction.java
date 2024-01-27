package qingzhou.console.controller.rest;

import qingzhou.console.ConsoleConstants;
import qingzhou.console.ConsoleI18n;
import qingzhou.console.I18n;
import qingzhou.console.ServerXml;
import qingzhou.console.Validator;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.page.PageBackendService;
import qingzhou.console.remote.RemoteClient;
import qingzhou.console.sdk.ConsoleSDK;
import qingzhou.crypto.KeyManager;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.ListModel;
import qingzhou.framework.api.ModelManager;
import qingzhou.framework.api.Response;
import qingzhou.console.RequestImpl;
import qingzhou.console.ResponseImpl;
import qingzhou.framework.pattern.Filter;
import qingzhou.framework.util.ExceptionUtil;
import qingzhou.framework.util.StringUtil;

import javax.naming.NameNotFoundException;
import java.net.SocketException;
import java.security.UnrecoverableKeyException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static qingzhou.console.impl.ConsoleWarHelper.getAppManager;

public class InvokeAction implements Filter<RestContext> {
    static {
        ConsoleI18n.addI18N("validator.fail", new String[]{"部分数据不合法", "en:Some of the data is not legitimate"});
    }

    InvokeAction() {
    }

    @Override
    public boolean doFilter(RestContext context) throws Exception {
        RequestImpl request = (RequestImpl) context.request;

        Response validationResponse = new ResponseImpl();
        boolean ok = Validator.validate(request, validationResponse);// 本地和远程走这统一的一次校验
        if (!ok) {
            context.response = validationResponse;
            if (StringUtil.isBlank(context.response.getMsg())) {
                context.response.setMsg(ConsoleI18n.getI18N(request.getI18nLang(), "validator.fail"));
            }
        } else {
            if (isBatchAction(request)) {
                context.response = invokeBatch(request);
            } else {
                context.response = invoke(request);
            }
        }

        return context.response.isSuccess(); // 触发后续的响应
    }

    private boolean isBatchAction(RequestImpl request) {
        String ids = request.getParameter(ListModel.FIELD_NAME_ID);
        if (StringUtil.isBlank(ids)) return false;

        ModelManager modelManager = ConsoleWarHelper.getAppStub(request.getAppName()).getModelManager();
        String[] actionNamesSupportBatch = modelManager.getActionNamesSupportBatch(request.getModelName());
        for (String batch : actionNamesSupportBatch) {
            if (batch.equals(request.getActionName())) return true;
        }

        return false;
    }

    private Response invokeBatch(RequestImpl request) {
        Response response = new ResponseImpl();
        int suc = 0;
        int fail = 0;
        StringBuilder errbuilder = new StringBuilder();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        String oid = request.getParameter(ListModel.FIELD_NAME_ID);
        for (String id : oid.split(ConsoleConstants.DATA_SEPARATOR)) {
            if (StringUtil.notBlank(id)) {
                id = ConsoleSDK.decodeId(id);
                request.setId(id);
                response = invoke(request);
                if (response.isSuccess()) {
                    suc++;
                } else {
                    String actionContextMsg = response.getMsg();
                    if (result.containsKey(actionContextMsg)) {
                        errbuilder.append(result.get(actionContextMsg));
                        errbuilder.append(ConsoleConstants.DATA_SEPARATOR);
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
        String appName = request.getAppName();
        String model = I18n.getString(appName, "model." + request.getModelName());
        String action = I18n.getString(appName, "model.action." + request.getModelName() + "." + request.getActionName());
        if (result.isEmpty()) {
            String resultMsg = ConsoleI18n.getI18N(I18n.getI18nLang(), "batch.ops.success", model, action, suc);
            response.setMsg(resultMsg);
        } else {
            response.setSuccess(suc > 0);
            errbuilder.append(ConsoleI18n.getI18N(I18n.getI18nLang(), "batch.ops.fail", model, action, suc, fail));
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

    public Response invoke(RequestImpl request) {
        try {
            Map<String, Response> responseOnNode = processRequest(request);
            for (Map.Entry<String, Response> entry : responseOnNode.entrySet()) {
                return entry.getValue(); // TODO 多条结果如何展示？
            }

            throw ExceptionUtil.unexpectedException("It should return at least one Response");
        } catch (Exception e) {
            Response response = new ResponseImpl();
            response.setSuccess(false);
            Set<Throwable> set = new HashSet<>();
            retrieveException(e, set);

            // 不要将异常msg输出到页面，有xss反射型漏洞，若需要，则应该尝试完善 errorcode
            String msg = null;
            for (Throwable temp : set) {
                //这些异常一般没有xss反射风险，所以其message可作为提示信息返回到客户端
                Class<?>[] exceptions = {SQLException.class, NameNotFoundException.class, SocketException.class,
                        UnrecoverableKeyException.class};
                for (Class<?> c : exceptions) {
                    if (c.isAssignableFrom(temp.getClass())) {
                        msg = temp.getMessage();
                    }
                }

                if (msg != null) {
                    break;
                }
            }

            if (msg == null) {
                msg = "Server exception, please check log for details.";
                e.printStackTrace();// 不能抛异常，否则到不了 view 处理
            }

            response.setMsg(msg);
            return response;
        }
    }

    private void retrieveException(Throwable t, Set<Throwable> set) {
        if (t == null) {
            return;
        }
        if (!set.add(t)) {
            return;
        }
        if (t.getCause() != null) {
            retrieveException(t.getCause(), set);
        }
        if (t.getSuppressed() != null) {
            for (Throwable thx : t.getSuppressed()) {
                retrieveException(thx, set);
            }
        }
    }

    public Map<String, Response> processRequest(RequestImpl request) throws Exception {
        Map<String, Response> resultOnNode = new HashMap<>();
        List<String> appNodes = new ArrayList<>();
        String manageType = request.getManageType();
        String appName = request.getAppName();
        if (ConsoleConstants.MANAGE_TYPE_NODE.equals(manageType)) {
            if (FrameworkContext.SYS_APP_NODE_AGENT.equals(appName)) {
                appNodes.add(FrameworkContext.SYS_NODE_LOCAL);
            }
        } else if (ConsoleConstants.MANAGE_TYPE_APP.equals(manageType)) {
            appNodes = getAppNodes(appName);
        }

        String remoteKey = null;
        for (String node : appNodes) {
            Response responseOnNode;
            if (node.equals(FrameworkContext.SYS_NODE_LOCAL)) {
                Response response = new ResponseImpl();
                getAppManager().getApp(appName).invoke(request, response);
                responseOnNode = response;
            } else {
                if (remoteKey == null) {
                    KeyManager keyManager = ConsoleWarHelper.getCryptoService().getKeyManager();
                    remoteKey = keyManager.getKeyOrElseInit(
                            PageBackendService.getSecureFile(ConsoleWarHelper.getDomain()),
                            "remoteKey",
                            null
                    );
                }

                Map<String, String> nodeById = ServerXml.get().getNodeById(node);
                String ip = nodeById.get("ip"); // 需和远程节点ip保持一致
                String port = nodeById.get("port");
                String remoteUrl = String.format("http://%s:%s", ip, port);
                responseOnNode = RemoteClient.sendReq(remoteUrl, request, remoteKey);
            }
            resultOnNode.put(node, responseOnNode);
        }

        return resultOnNode;
    }

    public static List<String> getAppNodes(String appName) {
        List<String> nodes = new ArrayList<>();
        if (FrameworkContext.SYS_APP_MASTER.equals(appName)) {
            nodes.add(FrameworkContext.SYS_NODE_LOCAL);
        } else {
            Map<String, String> res;
            try {
                res = getAppManager().getApp(FrameworkContext.SYS_APP_MASTER)
                        .getAppContext().getDataStore()
                        .getDataById(ConsoleConstants.MODEL_NAME_app, appName);
            } catch (Exception e) {
                throw ExceptionUtil.unexpectedException(e);
            }
            if (res == null || res.isEmpty()) {
                throw ExceptionUtil.unexpectedException("App [ " + appName + " ] not found.");
            }

            nodes.addAll(Arrays.asList(res.get("nodes").split(",")));
        }
        return nodes;
    }
}
