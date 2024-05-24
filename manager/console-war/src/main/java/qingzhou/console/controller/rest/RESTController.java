package qingzhou.console.controller.rest;

import qingzhou.console.ActionInvoker;
import qingzhou.console.ConsoleConstants;
import qingzhou.console.RequestImpl;
import qingzhou.console.ResponseImpl;
import qingzhou.console.controller.SystemController;
import qingzhou.console.i18n.I18n;
import qingzhou.console.login.LoginManager;
import qingzhou.console.page.PageBackendService;
import qingzhou.console.util.StringUtil;
import qingzhou.console.view.ViewManager;
import qingzhou.console.view.type.JsonView;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.engine.util.pattern.FilterPattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;

public class RESTController extends HttpServlet {
    public static final String REST_PREFIX = "/rest";
    public static final String INDEX_PATH = REST_PREFIX + "/" + ViewManager.htmlView + "/" + ConsoleConstants.MANAGE_TYPE_APP + "/" + "master" + "/" + "index" + "/" + "index";
    public static final String MSG_FLAG = "MSG_FLAG";
    public static final File TEMP_BASE_PATH = new File(SystemController.getModuleContext().getTemp(), "upload");

    public static String retrieveServletPathAndPathInfo(HttpServletRequest request) {
        return request.getServletPath() + (request.getPathInfo() != null ? request.getPathInfo() : "");
    }

    public static List<String> retrieveRestPathInfo(HttpServletRequest req) {
        List<String> result = new ArrayList<>();

        String uri = req.getPathInfo();
        if (uri != null) {
            String[] restTemp = uri.split("/");
            for (String r : restTemp) {
                if (StringUtil.notBlank(r)) {
                    result.add(r);
                }
            }
        }

        return result;
    }

    private static RESTController thisInstance;
    private final Filter<RestContext>[] filters = new Filter[]{
            new AsymmetricDecryptor(), // 解密前端的 password 类型的表单域
            new ValidationFilter(), // 参数校验

            // 执行具体的业务逻辑
            context -> {
                RestContext restContext = (RestContext) context;
                ResponseImpl response = ActionInvoker.getInstance().invokeAction(restContext.request);
                restContext.response = response;
                return response.isSuccess(); // 触发后续的响应
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

            RestContext context = new RestContext(req, resp, request, new ResponseImpl());
            FilterPattern.doFilter(context, filters);// filters 里面不能放入 view，因为 validator 失败后不会继续流入 view 里执行
            viewManager.render(context); // 最后作出响应
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (fileAttachments != null) {
                for (String fa : fileAttachments.values()) {
                    try {
                        File f = Utils.newFile(fa);
                        if (f.exists()) {
                            File parentFile = f.getParentFile();
                            if (parentFile.exists()
                                    && parentFile.getCanonicalPath().startsWith(TEMP_BASE_PATH.getCanonicalPath())) {
                                Utils.forceDelete(parentFile);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private RequestImpl buildRequest(HttpServletRequest req, HttpServletResponse resp, Map<String, String> fileAttachments) throws IOException {
        List<String> rest = retrieveRestPathInfo(req);
        int restDepth = 5;
        if (rest.size() < restDepth) { // must have model & action
            String msg = "Parameters missing, make sure to use the correct REST interface: " + req.getRequestURI();
            JsonView.responseErrorJson(resp, msg);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return null;
        }

        RequestImpl request = new RequestImpl();
        request.setViewName(rest.get(0));
        request.setManageType(rest.get(1));
        request.setAppName(rest.get(2));
        request.setModelName(rest.get(3));
        request.setActionName(rest.get(4));
        request.setUserName(LoginManager.getLoginUser(req.getSession(false)));
        request.setI18nLang(I18n.getI18nLang());

        StringBuilder id = new StringBuilder();
        if (rest.size() > restDepth) {
            id.append(rest.get(restDepth));
            for (int i = restDepth + 1; i < rest.size(); i++) {
                id.append("/").append(rest.get(i));// support ds jndi: jdbc/test
            }
            request.setId(PageBackendService.decodeId(id.toString()));
        }
        boolean actionFound = false;
        String[] actions = PageBackendService.getAppInfo(PageBackendService.getAppName(request))
                .getModelInfo(request.getModel())
                .getActionNames();
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
                data.put(k, String.join(",", v));
            }
        }

        if (fileAttachments != null) {
            data.putAll(fileAttachments);
        }

        request.setParameters(data);

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

                SimpleDateFormat DF = new SimpleDateFormat("yyyyMMddHHmmss");
                String time = DF.format(new Date());
                String fileName = Utils.newFile(part.getSubmittedFileName()).getName();
                File targetFile = Utils.newFile(TEMP_BASE_PATH, time, fileName);
                Utils.mkdirs(targetFile.getParentFile());

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
