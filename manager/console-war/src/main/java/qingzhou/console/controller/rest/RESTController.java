package qingzhou.console.controller.rest;

import qingzhou.api.FieldType;
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
import qingzhou.deployer.RequestImpl;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.engine.util.pattern.FilterPattern;
import qingzhou.registry.ModelFieldInfo;
import qingzhou.registry.ModelInfo;

import javax.servlet.ServletException;
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
    private static final String encodedFlag = "Encoded:";
    private static final String[] encodeFlags = {
            "#", "?", "&",// 一些不能在url中传递的参数
            ":", "%", "+", " ", "=", ",",
            "[", "]"
    };

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

            // 执行具体的业务逻辑
            context -> {
                RestContext restContext = (RestContext) context;
                List<Response> responseList = SystemController.getService(ActionInvoker.class).invokeAll(restContext.request);
                for (Response response : responseList) {
                    if (!response.isSuccess()) return false;
                }
                return true;
            }
    };
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
        ModelInfo modelInfo = SystemController.getAppInfo(SystemController.getAppName(request))
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

                data.put(k, String.join(",", v));
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
