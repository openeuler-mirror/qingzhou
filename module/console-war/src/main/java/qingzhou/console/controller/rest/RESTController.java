package qingzhou.console.controller.rest;

import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.app.App;
import qingzhou.app.RequestImpl;
import qingzhou.app.ResponseImpl;
import qingzhou.console.ActionInvoker;
import qingzhou.console.ConsoleConstants;
import qingzhou.console.I18n;
import qingzhou.console.RestContext;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.login.LoginManager;
import qingzhou.console.page.PageBackendService;
import qingzhou.console.view.ViewManager;
import qingzhou.console.view.type.JsonView;
import qingzhou.framework.pattern.Filter;
import qingzhou.framework.pattern.FilterPattern;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StringUtil;

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
    public static final String INDEX_PATH = REST_PREFIX + "/" + ViewManager.htmlView + "/" + ConsoleConstants.MANAGE_TYPE_APP + "/" + qingzhou.app.App.SYS_APP_MASTER + "/" + App.SYS_MODEL_INDEX + "/" + App.SYS_MODEL_INDEX;
    public static final String MSG_FLAG = "MSG_FLAG";
    public static final File TEMP_BASE_PATH = ConsoleWarHelper.getCache("upload");

    public static String retrieveServletPathAndPathInfo(HttpServletRequest request) {
        return request.getServletPath() + (request.getPathInfo() != null ? request.getPathInfo() : "");
    }

    public static List<String> retrieveRestPathInfo(HttpServletRequest req) {
        List<String> result = new ArrayList<>();

        String uri = req.getPathInfo();
        if (StringUtil.notBlank(uri)) {
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
            new AsymmetricDecryptor(),// 解密前端的 password 类型的表单域
            // 执行具体的业务逻辑
            context -> {
                RestContext restContext = (RestContext) context;
                Response response = ActionInvoker.getInstance().invokeAction(restContext.request);
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

            Request request = buildRequest(req, resp, fileAttachments);
            if (request == null) {
                return;
            }

            Response response = new ResponseImpl();
            RestContext context = new RestContext(req, resp, request, response);
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
                                FileUtil.forceDeleteQuietly(parentFile);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private Request buildRequest(HttpServletRequest req, HttpServletResponse resp, Map<String, String> fileAttachments) throws IOException {
        List<String> rest = retrieveRestPathInfo(req);
        int restDepth = 5;
        if (rest.size() < restDepth) { // must have model & action
            String msg = "Parameters missing, make sure to use the correct REST interface: " + req.getRequestURI();
            resp.getWriter().print(JsonView.buildErrorResponse(msg));
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
        String[] actions = ConsoleWarHelper.getAppStub(request.getAppName()).getModelManager().getActionNames(request.getModelName());
        for (String name : actions) {
            if (name.equals(request.getActionName())) {
                actionFound = true;
                break;
            }
        }

        if (!actionFound) {
            String msg = "Not Found: " + req.getRequestURI();
            resp.getWriter().print(JsonView.buildErrorResponse(msg));
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
            return null;
        }

        HashMap<String, String> data = new HashMap<>();
        Enumeration<String> parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String k = parameterNames.nextElement();
            String[] v = req.getParameterValues(k);
            if (v != null) {
                data.put(k, String.join(ConsoleConstants.DATA_SEPARATOR, v));
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
                    if (StringUtil.isBlank(checkName)
                            || checkName.contains("/") // 风险文件
                            || checkName.contains("\\") // 风险文件
                    ) {
                        throw new IllegalArgumentException("Illegal file name: " + checkName);
                    }
                }

                SimpleDateFormat DF = new SimpleDateFormat("yyyyMMddHHmmss");
                String time = DF.format(new Date());
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
