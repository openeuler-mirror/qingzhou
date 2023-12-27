package qingzhou.console.controller;

import qingzhou.api.console.data.Response;
import qingzhou.api.console.model.DownloadModel;
import qingzhou.api.console.model.ListModel;
import qingzhou.api.console.model.ShowModel;
import qingzhou.console.ConsoleUtil;
import qingzhou.console.RemoteClient;
import qingzhou.console.RequestImpl;
import qingzhou.console.ResponseImpl;
import qingzhou.console.ServerXml;
import qingzhou.console.Validator;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.sdk.ConsoleSDK;
import qingzhou.console.sec.SecureKey;
import qingzhou.console.util.Constants;
import qingzhou.console.util.StringUtil;
import qingzhou.framework.app.I18n;
import qingzhou.framework.impl.app.model.ModelManagerImpl;
import qingzhou.framework.pattern.Filter;

import javax.naming.NameNotFoundException;
import java.net.SocketException;
import java.security.UnrecoverableKeyException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InvokeAction implements Filter<RestContext> {
    private static final Set<String> noCheckActions = new HashSet<String>() {{
        add("create");
        add("add");
        add("list");
        add("addfavorite");
        add("cancelfavorites");
        add(DownloadModel.ACTION_NAME_DOWNLOADLIST);
        add(DownloadModel.ACTION_NAME_DOWNLOADFILE);
    }};

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
        return StringUtil.notBlank(ids) && ids.contains(Constants.DATA_SEPARATOR);
    }

    private Response invokeBatch(RequestImpl request) {
        Response response = new ResponseImpl();
        int suc = 0;
        int fail = 0;
        StringBuilder errbuilder = new StringBuilder();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        String oid = request.getParameter(ListModel.FIELD_NAME_ID);
        for (String id : oid.split(Constants.DATA_SEPARATOR)) {
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
                        errbuilder.append(Constants.DATA_SEPARATOR);
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
        String appName = ServerXml.getAppName(request.getTargetType(), request.getTargetName());
        String model = I18n.getString(appName, "model." + request.getModelName());
        String action = I18n.getString(appName, "model.action." + request.getModelName() + "." + request.getActionName());
        if (result.isEmpty()) {
            String resultMsg = String.format(I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "batch.ops.success"), model, action, suc);
            response.setMsg(resultMsg);
        } else {
            response.setSuccess(suc > 0);
            errbuilder.append(String.format(I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "batch.ops.fail"), model, action, suc, fail));
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
        try {
            String actionName = request.getActionName();// 事先提取出来，否则后续可能被修改，再拿可能会变
            String modelName = request.getModelName();// 事先提取出来，否则后续可能被修改，再拿可能会变
            if (isRemoteCall(request)) {
                List<String> uploadFiles = new ArrayList<>();
                try {
                    Map<String, String> fileAttachments = request.getFileAttachments();
                    if (fileAttachments != null) {
                        String uploadUrl = buildRemoteRequestUrl(request, Constants.uploadPath);
                        for (Map.Entry<String, String> entry : fileAttachments.entrySet()) {
                            String uploadFile = RemoteClient.uploadFile(uploadUrl, entry.getValue());
                            uploadFiles.add(uploadFile);
                            request.updateParameter(entry.getKey(), uploadFile);
                        }
                    }
                    String url = buildRemoteRequestUrl(request, Constants.remotePath);
                    String remoteKey = SecureKey.getSecureKey(ConsoleWarHelper.getDomain(), SecureKey.remoteKeyName);// todo 这里应该用远端的 key ？
                    response = RemoteClient.sendReq(url, request, remoteKey);
                } finally {
                    if (!uploadFiles.isEmpty()) {
                        String delFileUrl = buildRemoteRequestUrl(request, Constants.deleteFilePath);
                        RemoteClient.deleteFiles(delFileUrl, uploadFiles);
                    }
                }
            } else {
                String appName = ConsoleUtil.getAppName(request.getTargetType(), request.getTargetName());
                ModelManagerImpl manager = (ModelManagerImpl) ConsoleUtil.getModelManager(appName);
                Class<?> modelClass = manager.getModelClass(modelName);
                if (modelClass != null) {
                    if (ListModel.class.isAssignableFrom(modelClass)) {
                        if (!noCheckActions.contains(actionName)) {
                            Response responseTemp = new ResponseImpl();
                            ShowModel showModel = manager.getModelInstance(modelName);
                            showModel.show(request, responseTemp);
                            if (responseTemp.modelData().getDataList().isEmpty()) {
                                response.setSuccess(false);
                                String msg = String.format(
                                        I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "validator.notexist"),
                                        I18n.getString(appName, "model." + modelName));
                                response.setMsg(msg);
                            }
                        }
                    }

                    if (Validator.validate(request, response, manager)) {
                        manager.invokeAction(request, response);
                    }
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

    private boolean isRemoteCall(RequestImpl request) {
        String targetName = request.getTargetName();
        return StringUtil.notBlank(targetName)
                && !Constants.QINGZHOU_MASTER_APP_NAME.equals(targetName)
                && !Constants.QINGZHOU_DEFAULT_APP_NAME.equals(targetName);
    }

    private String buildRemoteRequestUrl(RequestImpl request, String path) {
        Map<String, String> instanceById = ServerXml.get().getInstanceById(request.getTargetName());// todo：是没有没有兼顾 “集群”的“管理”
        if (instanceById == null) {
            return null;
        }

        String nodeName = instanceById.get("node");

        String ip;
        String port;
//    todo    if (Constants.QINGZHOU_DEFAULT_NODE_NAME.equals(nodeName)) {
//            ip = ServerUtil.getGlobalIp(); // 需和默认节点ip保持一致
//        } else {
//            Map<String, String> nodeById = ServerXml.get().getNodeById(nodeName);
//            ip = nodeById.get("ip"); // 需和远程节点ip保持一致
//        }
        Map<String, String> nodeById = ServerXml.get().getNodeById(nodeName);
        ip = nodeById.get("ip"); // 需和远程节点ip保持一致

        port = instanceById.get("port");
        if (StringUtil.isBlank(ip) || StringUtil.isBlank(port)) {
            return null;
        }

        return String.format("http://%s:%s%s%s", ip, port, Constants.remoteApp, path);
    }
}
