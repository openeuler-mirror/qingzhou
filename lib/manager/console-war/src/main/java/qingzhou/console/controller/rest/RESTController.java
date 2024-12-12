package qingzhou.console.controller.rest;

import qingzhou.api.InputType;
import qingzhou.api.MsgLevel;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.config.console.User;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.console.login.LoginManager;
import qingzhou.console.view.ViewManager;
import qingzhou.console.view.type.JsonView;
import qingzhou.core.deployer.ActionInvoker;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.deployer.ResponseImpl;
import qingzhou.core.registry.ModelFieldInfo;
import qingzhou.core.registry.ModelInfo;
import qingzhou.crypto.Base32Coder;
import qingzhou.crypto.Base64Coder;
import qingzhou.crypto.CryptoService;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.engine.util.pattern.FilterPattern;
import qingzhou.logger.Logger;

import javax.servlet.http.*;
import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RESTController extends HttpServlet {
    public static final String MSG_FLAG = "MSG_FLAG";
    public static final File TEMP_BASE_PATH = new File(SystemController.getModuleContext().getTemp(), "upload");
    private static final String ENCODED_FLAG = "Encoded:";
    private static final String[] ENCODE_FLAGS = {
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
            for (String flag : ENCODE_FLAGS) {
                if (id.contains(flag)) {
                    Base32Coder base32Coder = SystemController.getService(CryptoService.class).getBase32Coder();
                    return ENCODED_FLAG + base32Coder.encode(id.getBytes(StandardCharsets.UTF_8)); // for #NC-558 特殊字符可能编码了
                }
            }
        } catch (Exception ignored) {
        }

        return id; // 出错，表示 rest 接口，没有编码
    }

    // 启动参数(如 -XX:+DisableExplicitGC )有特殊字符，编码后放在url里作参数，因此需要解码
    public static String decodeId(String encodeId) {
        try {
            if (encodeId.startsWith(ENCODED_FLAG)) {
                Base32Coder base32Coder = SystemController.getService(CryptoService.class).getBase32Coder();
                return new String(base32Coder.decode(encodeId.substring(ENCODED_FLAG.length())), StandardCharsets.UTF_8); // for #NC-558 特殊字符可能编码了
            }
        } catch (Exception ignored) {
        }
        return encodeId; // 出错，表示 rest 接口，没有编码
    }

    private static RESTController thisInstance;
    private final Filter<RestContext>[] filters = new Filter[]{
            new SecurityController(),
            new ResetPassword(),
//            new BackFilter(), // 因前端 子菜单跳转 等功能支持，暂无法结合后端历史请求处理，会导致前端控制不到显示区域和菜单定位等问题
            new ParameterFilter(), // 解密前端的 password 类型的表单域
            new ActionFilter(),
            new ValidationFilter(), // 参数校验
            new EchoFilter(), // 数据回显
            // 执行具体的业务逻辑
            context -> {
                RestContext restContext = (RestContext) context;
                RequestImpl request = restContext.request;
                Map<String, Response> instanceResult = SystemController.getService(ActionInvoker.class).invoke(request);
                responseMsg(instanceResult, request);
                return true;
            }
    };

    private void responseMsg(Map<String, Response> instanceResult, RequestImpl request) {
        ResponseImpl response = (ResponseImpl) request.getResponse();
        Map<String, Response> responseList = request.getInvokeOnInstances();
        if (responseList != null && !responseList.isEmpty()) {
            instanceResult = responseList;
        }

        if (instanceResult.size() == 1) {  // 非批量操作，不需要重组响应信息
            response = (ResponseImpl) instanceResult.values().iterator().next();
            request.setResponse(response);
        } else {
            Map<String, ResponseImpl> failed = new LinkedHashMap<>();
            int suc = 0;
            int fail = 0;
            for (Map.Entry<String, Response> e : instanceResult.entrySet()) {
                ResponseImpl check = (ResponseImpl) e.getValue();
                if (check.isSuccess()) {
                    suc += 1;
                } else {
                    fail += 1;
                    failed.put(e.getKey(), check);
                }
            }

            response.setSuccess(suc > 0); // 至少有一个成功，则认为整体是成功的

            MsgLevel msgLevel = suc > 0
                    ? (fail > 0 ? MsgLevel.WARN : MsgLevel.INFO)
                    : MsgLevel.ERROR;
            response.setMsgLevel(msgLevel);

            StringBuilder errorMsg = new StringBuilder(String.format(I18n.getKeyI18n("batch.ops.fail"), suc, fail));
            for (Map.Entry<String, ResponseImpl> e : failed.entrySet()) {
                String instance = e.getKey();
                String msg = e.getValue().getMsg();
                errorMsg.append(instance).append(": ").append(msg);
            }
            response.setMsg(errorMsg.toString());
        }

        // 完善响应的 msg
        if (response.getMsg() == null) {
            response.setMsg(defaultMsg(response.isSuccess(), request));
        }

        // 完善响应的 msg type
        if (response.getMsgLevel() == null) {
            response.setMsgLevel(response.isSuccess() ? MsgLevel.INFO : MsgLevel.ERROR);
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

    private final ViewManager viewManager = new ViewManager();

    @Override
    public void init() {
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
            SystemController.getService(Logger.class).error(e.getMessage(), e);
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
        HttpSession session = req.getSession();
        request.addSessionParameterListener(session::setAttribute);
        request.setViewName(rest.get(0));
        request.setAppName(rest.get(1));
        request.setModelName(rest.get(2));
        request.setActionName(rest.get(3));
        User loggedUser = LoginManager.getLoggedUser(session);
        if (loggedUser != null) {
            request.setUserName(loggedUser.getName());
        }
        request.setI18nLang(I18n.getI18nLang());

        ModelInfo modelInfo = SystemController.getAppInfo(request.getApp()).getModelInfo(request.getModel());

        StringBuilder id = new StringBuilder();
        if (rest.size() > restDepth) {
            id.append(rest.get(restDepth));
            for (int i = restDepth + 1; i < rest.size(); i++) {
                id.append("/").append(rest.get(i)); // support ds jndi: jdbc/test
            }
            String decodeId = RESTController.decodeId(id.toString());
            request.setId(decodeId);
            // Update 更新操作参数里需要id
            request.getParameters().put(modelInfo.getIdField(), decodeId);
        }

        HashMap<String, String> data = new HashMap<>();
        Enumeration<String> parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String k = parameterNames.nextElement();
            String[] v = req.getParameterValues(k);
            if (v != null) {
                ModelFieldInfo modelFieldInfo = modelInfo.getModelFieldInfo(k);
                if (modelFieldInfo != null && InputType.kv == modelFieldInfo.getInputType()) {
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

        data.forEach((k, v) -> request.getParameters().put(k, v));

        Enumeration<String> attributeNames = session.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String key = attributeNames.nextElement();
            Object value = session.getAttribute(key);
            if (value instanceof String) {
                request.parametersForSession().put(key, (String) value);
            }
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
