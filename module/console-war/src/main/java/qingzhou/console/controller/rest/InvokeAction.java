package qingzhou.console.controller.rest;

import qingzhou.console.ConsoleUtil;
import qingzhou.console.ServerXml;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.remote.RemoteClient;
import qingzhou.console.sdk.ConsoleSDK;
import qingzhou.crypto.KeyManager;
import qingzhou.framework.api.ListModel;
import qingzhou.framework.api.Response;
import qingzhou.framework.console.ConsoleConstants;
import qingzhou.framework.console.I18n;
import qingzhou.framework.console.RequestImpl;
import qingzhou.framework.console.ResponseImpl;
import qingzhou.framework.pattern.Filter;
import qingzhou.framework.util.StringUtil;
import qingzhou.remote.RemoteConstants;

import javax.naming.NameNotFoundException;
import java.net.SocketException;
import java.security.UnrecoverableKeyException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InvokeAction implements Filter<RestContext> {
    InvokeAction() {
    }

    @Override
    public boolean doFilter(RestContext context) throws Exception {
        RequestImpl request = (RequestImpl) context.request;
        if (isBatchAction(request)) {
            context.response = invokeBatch(request);
        } else {
            context.response = invoke(request);
        }
        return context.response.isSuccess(); // 触发后续的响应
    }

    private boolean isBatchAction(RequestImpl request) {
        String ids = request.getParameter(ListModel.FIELD_NAME_ID);
        return StringUtil.notBlank(ids) && ids.contains(ConsoleConstants.DATA_SEPARATOR);
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
            String resultMsg = String.format(I18n.getString(ConsoleConstants.MASTER_APP_NAME, "batch.ops.success"), model, action, suc);
            response.setMsg(resultMsg);
        } else {
            response.setSuccess(suc > 0);
            errbuilder.append(String.format(I18n.getString(ConsoleConstants.MASTER_APP_NAME, "batch.ops.fail"), model, action, suc, fail));
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
        Response response = new ResponseImpl();
        List<String> nodes = new ArrayList<>();
        try {
            String targetType = request.getTargetType();
            String targetName = request.getTargetName();
            switch (TargetType.valueOf(targetType)) {
                case node:
                    if (!targetName.equals(ConsoleConstants.LOCAL_NODE_NAME)) {
                        nodes.add(targetName);
                    }
                    break;
                case cluster:
                    Map<String, String> cluster = ServerXml.get().getClusterById(targetName);
                    nodes.addAll(Arrays.asList(cluster.get("nodes").split(ConsoleConstants.DATA_SEPARATOR)));
                    break;
                default:
                    throw new IllegalStateException("Unknown TargetType: " + targetType);
            }

            if (nodes.isEmpty()) {
                ConsoleWarHelper.getAppInfoManager().getAppInfo(request.getAppName()).invokeAction(request, response);
            } else {
                KeyManager keyManager = ConsoleWarHelper.getCryptoService().getKeyManager();
                String remoteKey = keyManager.getKeyOrElseInit(ConsoleUtil.getSecureFile(ConsoleWarHelper.getDomain()), RemoteConstants.REMOTE_KEY_NAME, null);
                for (String node : nodes) {
                    String remoteUrl = buildRemoteUrl(node);
                    response = RemoteClient.sendReq(remoteUrl, request, remoteKey);// todo : 每个 response 会被后来的覆盖掉，导致状态信息丢失
                }
            }
        } catch (Exception e) {
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
        }
        return response;
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

    private String buildRemoteUrl(String nodeName) {
        Map<String, String> nodeById = ServerXml.get().getNodeById(nodeName);
        String ip = nodeById.get("ip"); // 需和远程节点ip保持一致
        String port = nodeById.get("port");
        return String.format("http://%s:%s", ip, port);
    }
}
