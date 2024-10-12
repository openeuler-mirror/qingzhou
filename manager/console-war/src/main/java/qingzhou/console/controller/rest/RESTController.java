package qingzhou.console.controller.rest;

import qingzhou.api.FieldType;
import qingzhou.api.MsgLevel;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.console.login.LoginManager;
import qingzhou.console.view.ViewManager;
import qingzhou.console.view.type.JsonView;
import qingzhou.crypto.Base32Coder;
import qingzhou.crypto.Base64Coder;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.engine.util.pattern.FilterPattern;
import qingzhou.registry.ModelFieldInfo;
import qingzhou.registry.ModelInfo;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RESTController extends HttpServlet {
    public static final String MSG_FLAG = "MSG_FLAG";
    public static final File TEMP_BASE_PATH = new File(SystemController.getModuleContext().getTemp(), "upload");
    private static final String encodedFlag = "Encoded:";
    private static final String[] encodeFlags = {
            "#", "?", "&",// 一些不能在url中传递的参数
            ":", "%", "+", " ", "=", ",",
            "[", "]"
    };

    static {
        I18n.addKeyI18n("batch.ops.fail", new String[]{"成功 %s 个，失败 %s 个，失败详情：", "en:Success %s, failure %s, failure details: "});
    }

    public static String getReqUri(HttpServletRequest request) {
        return request.getServletPath() + (request.getPathInfo() != null ? request.getPathInfo() : "");
    }

    private static List<String> detectRest(HttpServletRequest req) {
        List<String> result = new ArrayList<>();

        String uri = req.getPathInfo();
        if (uri != null) {
            String[] restTemp = uri.split("/");
            for (String r : restTemp) {
                if (Utils.notBlank(r)) {
                    result.add(r);
                }
            }
        }

        return result;
    }

    public static String encodeURL(HttpServletResponse response, String url) {
        return response.encodeURL(url);
    }

    // 启动参数(如 -XX:+DisableExplicitGC )有特殊字符，不能在url里作参数，因此需要编码
    public static String encodeId(String id) {
        try {
            for (String flag : encodeFlags) {
                if (id.contains(flag)) {
                    Base32Coder base32Coder = SystemController.getService(CryptoService.class).getBase32Coder();
                    return encodedFlag + base32Coder.encode(id.getBytes(StandardCharsets.UTF_8)); // for #NC-558 特殊字符可能编码了
                }
            }
        } catch (Exception ignored) {
        }

        return id; // 出错，表示 rest 接口，没有编码
    }

    // 启动参数(如 -XX:+DisableExplicitGC )有特殊字符，编码后放在url里作参数，因此需要解码
    public static String decodeId(String encodeId) {
        try {
            if (encodeId.startsWith(encodedFlag)) {
                Base32Coder base32Coder = SystemController.getService(CryptoService.class).getBase32Coder();
                return new String(base32Coder.decode(encodeId.substring(encodedFlag.length())), StandardCharsets.UTF_8); // for #NC-558 特殊字符可能编码了
            }
        } catch (Exception ignored) {
        }
        return encodeId; // 出错，表示 rest 接口，没有编码
    }

    private static RESTController thisInstance;
    private final Filter<RestContext>[] filters = new Filter[]{
            new ResetPassword(),
            new ParameterFilter(), // 解密前端的 password 类型的表单域
            new ActionFilter(),
            new ValidationFilter(), // 参数校验
            new EchoFilter(), // 数据回显
            new RedirectFilter(), // 下载远端重定向资源
            // 执行具体的业务逻辑
            context -> {
                RestContext restContext = (RestContext) context;

                Object[] result;
                //批量操作判断，过滤器结束后，进入模块 action 处理
                boolean isBatch = restContext.batchIds != null;
                if (isBatch) {
                    result = invokeBatch(restContext);
                } else {
                    result = invoke(restContext);
                }
                responseMsg(result, restContext.request, isBatch);
                return true;
            }
    };

    private void responseMsg(Object[] totalResult, RequestImpl request, boolean isBatch) {
        Response response = request.getResponse();
        List<Map<String, String>> dataList = response.getDataList();
        if (dataList.isEmpty()) {
            dataList.addAll((Collection<? extends Map<String, String>>) totalResult[3]);
        }
        int suc = ((int) totalResult[0]);
        int fail = ((int) totalResult[1]);
        Map<String, String> totalError = (Map<String, String>) totalResult[2];
        if (!isBatch) {  // 非批量操作，不需要重组响应信息
            if (response.getMsg() == null) { // 完善响应的 msg
                if (fail == 1) {
                    response.setMsg(totalError.isEmpty() ? "" : totalError.keySet().stream().findFirst().get());
                    response.setMsgType(MsgLevel.error);
                } else {
                    response.setMsg(defaultMsg(response.isSuccess(), request));
                }
            }
        } else {
            response.setSuccess(suc > 0); // 至少有一个成功，则认为整体是成功的
            if (totalError.isEmpty()) {
                response.setMsg(defaultMsg(response.isSuccess(), request)); // 以整体的成功与否来设置默认消息
            } else {
                StringBuilder errorMsg = new StringBuilder(String.format(I18n.getKeyI18n("batch.ops.fail"), suc, fail));
                for (Map.Entry<String, String> entry : totalError.entrySet()) {
                    String instance = entry.getKey();
                    String msg = entry.getValue();
                    errorMsg.append(msg).append("= {").append(instance).append("}; ");
                }
                response.setMsg(errorMsg.toString());
            }

            MsgLevel msgLevel = suc > 0
                    ? (fail > 0 ? MsgLevel.warn : MsgLevel.info)
                    : MsgLevel.error;
            response.setMsgType(msgLevel);
        }

        // 完善响应的 msg type
        if (response.getMsgType() == null) {
            response.setMsgType(response.isSuccess() ? MsgLevel.info : MsgLevel.error);
        }
    }

    private String defaultMsg(boolean success, Request request) {
        String appName = request.getApp();
        String SP = I18n.isZH() ? "" : " ";
        String msg = success ? I18n.getKeyI18n("msg.success") : I18n.getKeyI18n("msg.fail");
        String model = I18n.getModelI18n(appName, "model." + request.getModel());
        String action = I18n.getModelI18n(appName, "model.action." + request.getModel() + "." + request.getAction());
        String operation = Objects.equals(model, action) ? model : model + SP + action;
        return operation + SP + msg;
    }

    private Object[] invoke(RestContext restContext) {
        int suc = 0;
        int fail = 0;
        Map<String, String> result = new HashMap<>();

        RequestImpl request = restContext.request;
        List<Response> responseList = SystemController.getService(ActionInvoker.class).invokeAll(request);
        StringBuilder errorMsg = new StringBuilder();
        for (Response response : responseList) {
            if (response.isSuccess()) {
                suc++;
            } else {
                String errMsg = response.getMsg();
                if (result.containsKey(errMsg)) {
                    errorMsg.append(result.get(errMsg));
                    errorMsg.append(DeployerConstants.DEFAULT_DATA_SEPARATOR);
                    errorMsg.append(request.getId());
                    result.put(errMsg, errorMsg.toString());
                    errorMsg.setLength(0);
                } else {
                    result.put(errMsg, request.getId());
                }
                fail++;
            }
        }

        return new Object[]{suc, fail, result, responseList.get(0).getDataList()};
    }

    private Object[] invokeBatch(RestContext restContext) {
        int suc = 0;
        int fail = 0;
        Map<String, String> result = new HashMap<>();
        List<Map<String, String>> dataList = null;
        StringBuilder errorMsg = new StringBuilder();
        for (String id : restContext.batchIds) {
            restContext.request.setId(id);
            Object[] batchOne = invoke(restContext);
            suc += ((int) batchOne[0]);
            fail += ((int) batchOne[1]);
            Map<String, String> oneMap = (Map<String, String>) batchOne[2];
            if (dataList == null) {
                dataList = (List<Map<String, String>>) batchOne[3];
            }
            for (String k : oneMap.keySet()) {
                if (result.containsKey(k)) {
                    errorMsg.append(result.get(k));
                    errorMsg.append(DeployerConstants.DEFAULT_DATA_SEPARATOR);
                    errorMsg.append(oneMap.get(k));
                    result.put(k, errorMsg.toString());
                    errorMsg.setLength(0);
                } else {
                    result.put(k, oneMap.get(k));
                }
            }
        }

        return new Object[]{suc, fail, result, dataList};
    }

    private final ViewManager viewManager = new ViewManager();

    @Override
    public void init() throws ServletException {
        super.init();
        thisInstance = this;
    }

    public static void invokeReq(HttpServletRequest req, HttpServletResponse resp) {
        thisInstance.doRest(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        doRest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        doRest(req, resp);
    }

    private void doRest(HttpServletRequest req, HttpServletResponse resp) {
        Map<String, String> fileAttachments = null;
        try {
            fileAttachments = prepareUploadFiles(req);// 必须在最开始处理上传文件！！！一旦调用了 request.getParameter方法就会丢失上传文件内容

            RequestImpl request = buildRequest(req, resp, fileAttachments);
            if (request == null) {
                return;
            }
            RestContext context = new RestContext(req, resp, request);

            FilterPattern.doFilter(context, filters);// filters 里面不能放入 view，因为 validator 失败后不会继续流入 view 里执行
            viewManager.render(context); // 最后作出响应
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (fileAttachments != null) {
                for (String fa : fileAttachments.values()) {
                    try {
                        File f = FileUtil.newFile(fa);
                        if (f.exists()) {
                            File parentFile = f.getParentFile();
                            if (parentFile.exists()
                                    && parentFile.getCanonicalPath().startsWith(TEMP_BASE_PATH.getCanonicalPath())) {
                                FileUtil.forceDelete(parentFile);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private RequestImpl buildRequest(HttpServletRequest req, HttpServletResponse resp, Map<String, String> fileAttachments) throws IOException {
        List<String> rest = detectRest(req);
        int restDepth = 4;
        if (rest.size() < restDepth) { // must have model & action
            String msg = "Parameters missing, make sure to use the correct REST interface: " + req.getRequestURI();
            JsonView.responseErrorJson(resp, msg);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return null;
        }

        RequestImpl request = new RequestImpl();
        request.addSessionParameterListener((key, val) -> req.getSession().setAttribute(key, val));
        request.setViewName(rest.get(0));
        request.setAppName(rest.get(1));
        request.setModelName(rest.get(2));
        request.setActionName(rest.get(3));
        request.setUserName(LoginManager.getLoginUser(req));
        request.setI18nLang(I18n.getI18nLang());

        StringBuilder id = new StringBuilder();
        if (rest.size() > restDepth) {
            id.append(rest.get(restDepth));
            for (int i = restDepth + 1; i < rest.size(); i++) {
                id.append("/").append(rest.get(i)); // support ds jndi: jdbc/test
            }
            request.setId(RESTController.decodeId(id.toString()));
        }
        boolean actionFound = false;
        ModelInfo modelInfo = SystemController.getAppInfo(request.getApp())
                .getModelInfo(request.getModel());
        String[] actions = modelInfo.getActionNames();
        for (String name : actions) {
            if (name.equals(request.getAction())) {
                actionFound = true;
                break;
            }
        }

        if (!actionFound) {
            String msg = "Not Found: " + req.getRequestURI();
            JsonView.responseErrorJson(resp, msg);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
            return null;
        }

        HashMap<String, String> data = new HashMap<>();
        Enumeration<String> parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String k = parameterNames.nextElement();
            String[] v = req.getParameterValues(k);
            if (v != null) {
                ModelFieldInfo modelFieldInfo = modelInfo.getModelFieldInfo(k);
                if (modelFieldInfo != null && FieldType.kv.name().equals(modelFieldInfo.getType())) {
                    for (int i = 0; i < v.length; i++) {// 前端页面的 kv 组件会对此进行 Base64加密，在这里进行解密，解密异常不处理，传递原始数据
                        v[i] = v[i].trim();
                        try {
                            Base64Coder base64Coder = SystemController.getService(CryptoService.class).getBase64Coder();
                            v[i] = new String(base64Coder.decode(v[i]), StandardCharsets.UTF_8);
                        } catch (Exception ignored) {
                        }
                    }
                }

                String sp = modelFieldInfo != null ? modelFieldInfo.getSeparator() : ",";
                data.put(k, String.join(sp, v));
            }
        }

        if (fileAttachments != null) {
            data.putAll(fileAttachments);
        }

        data.forEach(request::setParameter);

        HttpSession session = req.getSession(false);
        if (session != null) {
            Map<String, String> paramsInSession = new HashMap<>();
            Enumeration<String> attributeNames = session.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String key = attributeNames.nextElement();
                paramsInSession.put(key, String.valueOf(session.getAttribute(key)));
            }
            request.getParametersInSession().putAll(paramsInSession);
        }

        request.setCachedModelInfo(modelInfo);

        return request;
    }

    private Map<String, String> prepareUploadFiles(HttpServletRequest request) throws Exception {
        if (request.getContentType() == null || !request.getContentType().startsWith("multipart/form-data")) {
            return null;
        }

        Map<String, String> fileAttachments = new HashMap<>();
        for (Part part : request.getParts()) {
            try {
                if (part.getContentType() == null) {
                    continue;
                }

                String[] checkNames = {
                        part.getSubmittedFileName(),
                        part.getName()
                };
                for (String checkName : checkNames) {
                    if (checkName == null
                            || checkName.isEmpty()
                            || checkName.contains("/") // 风险文件
                            || checkName.contains("\\") // 风险文件
                    ) {
                        throw new IllegalArgumentException("Illegal file name: " + checkName);
                    }
                }

                String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                String fileName = FileUtil.newFile(part.getSubmittedFileName()).getName();
                File targetFile = FileUtil.newFile(TEMP_BASE_PATH, time, fileName);
                FileUtil.mkdirs(targetFile.getParentFile());

                try (ReadableByteChannel readChannel = Channels.newChannel(part.getInputStream());
                     FileChannel writeChannel = FileChannel.open(targetFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                    writeChannel.transferFrom(readChannel, 0, part.getSize());
                }

                fileAttachments.put(part.getName(), targetFile.getAbsolutePath());
            } finally {
                try {
                    part.delete();
                } catch (IOException ignored) {
                }
            }
        }

        return fileAttachments;
    }
}
