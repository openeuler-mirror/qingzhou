package qingzhou.console;

import qingzhou.api.Request;
import qingzhou.api.type.Listable;
import qingzhou.api.type.Showable;
import qingzhou.console.controller.SystemController;
import qingzhou.console.i18n.ConsoleI18n;
import qingzhou.console.i18n.I18n;
import qingzhou.console.page.PageBackendService;
import qingzhou.console.remote.RemoteClient;
import qingzhou.deployer.Deployer;
import qingzhou.logger.Logger;
import qingzhou.registry.InstanceInfo;
import qingzhou.registry.ModelInfo;
import qingzhou.registry.Registry;

import javax.naming.NameNotFoundException;
import java.net.SocketException;
import java.security.UnrecoverableKeyException;
import java.sql.SQLException;
import java.util.*;

public class ActionInvoker {
    private static final ActionInvoker instance = new ActionInvoker();

    public static ActionInvoker getInstance() {
        return instance;
    }

    private ActionInvoker() {
    }

    public ResponseImpl invokeAction(Request request) {
        if (isBatchAction(request)) {
            return invokeBatch(request);
        } else {
            return invoke(request);
        }
    }

    private boolean isBatchAction(Request request) {
        String ids = request.getParameter(Listable.FIELD_NAME_ID);
        if (ids == null) return false;

        ModelInfo modelInfo = SystemController.getAppInfo(request.getApp()).getModelInfo(request.getModel());
        String[] actionNamesSupportBatch = modelInfo.getBatchActionNames();
        for (String batch : actionNamesSupportBatch) {
            if (batch.equals(request.getAction())) return true;
        }

        return false;
    }

    private ResponseImpl invokeBatch(Request request) {
        ResponseImpl response = new ResponseImpl();
        int suc = 0;
        int fail = 0;
        StringBuilder errbuilder = new StringBuilder();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        String oid = request.getParameter(Listable.FIELD_NAME_ID);
        for (String id : oid.split(",")) {
            if (!id.isEmpty()) {
                id = PageBackendService.decodeId(id);
                ((RequestImpl) request).setId(id);
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
        ((RequestImpl) request).setId(oid);
        String appName = PageBackendService.getAppName(request);
        String model = I18n.getString(appName, "model." + request.getModel());
        String action = I18n.getString(appName, "model.action." + request.getModel() + "." + request.getAction());
        if (result.isEmpty()) {
            String resultMsg = ConsoleI18n.getI18n(I18n.getI18nLang(), "batch.ops.success", model, action, suc);
            response.setMsg(resultMsg);
        } else {
            response.setSuccess(suc > 0);
            errbuilder.append(ConsoleI18n.getI18n(I18n.getI18nLang(), "batch.ops.fail", model, action, suc, fail));
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

    private ResponseImpl invoke(Request request) {
        try {
            Map<String, ResponseImpl> responseOnNode = processRequest(request);
            for (Map.Entry<String, ResponseImpl> entry : responseOnNode.entrySet()) {
                return entry.getValue(); // TODO 多条结果如何展示？
            }

            throw new IllegalStateException("It should return at least one Response");
        } catch (Exception e) {
            ResponseImpl response = new ResponseImpl();
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
                // 不能抛异常，否则到不了 view 处理
                SystemController.getService(Logger.class).warn(msg, e);
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

    private Map<String, ResponseImpl> processRequest(Request request) throws Exception {
        Map<String, ResponseImpl> resultOnNode = new HashMap<>();
        List<String> appNodes = new ArrayList<>();
        String manageType = ((RequestImpl) request).getManageType();
        String appName = request.getApp();
        if (ConsoleConstants.MANAGE_TYPE_NODE.equals(manageType)) {
            appNodes.add(appName);
        } else if (ConsoleConstants.MANAGE_TYPE_APP.equals(manageType)) {
            appNodes = getAppNodes(appName);
        }

        for (String node : appNodes) {
            ResponseImpl responseOnNode;
            if (node.equals("local")) {
                responseOnNode = new ResponseImpl();
                SystemController.getService(Deployer.class)
                        .getApp(PageBackendService.getAppName(request))
                        .invoke(request, responseOnNode);
            } else {
                InstanceInfo instanceInfo = SystemController.getService(Registry.class).getInstanceInfo(node);
                String remoteUrl = String.format("http://%s:%s",
                        instanceInfo.getHost(), //todo: 多网卡的 agent 会注册多个 ip 地址，考虑优化为 被动模式 使得master不依赖agent的ip？（ps:agent在docker容器里，master可能无法放到到agent的ip）
                        instanceInfo.getPort());
                responseOnNode = RemoteClient.sendReq(remoteUrl, request, instanceInfo.getKey());
            }
            resultOnNode.put(node, responseOnNode);
        }

        return resultOnNode;
    }

    private List<String> getAppNodes(String appName) throws Exception {
        List<String> nodes = new ArrayList<>();
        if ("master".equals(appName)) {
            nodes.add("local");
        } else {
            RequestImpl request = new RequestImpl();
            ResponseImpl response = new ResponseImpl();
            request.setAppName("master");
            request.setModelName("app");
            request.setActionName(Showable.ACTION_NAME_SHOW);
            request.setId(appName);
            SystemController.invokeLocalApp(request, response);
            List<Map<String, String>> dataList = response.getDataList();
            if (dataList != null && !dataList.isEmpty()) {
                Map<String, String> res = dataList.get(0);
                nodes.addAll(Arrays.asList(res.get("nodes").split(",")));
            } else {
                throw new IllegalArgumentException("App [ " + appName + " ] not found.");
            }

        }
        return nodes;
    }
}
