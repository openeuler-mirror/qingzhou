package qingzhou.console.controller.rest;

import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;
import qingzhou.console.ConsoleUtil;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.impl.RequestImpl;
import qingzhou.console.impl.ResponseImpl;
import qingzhou.console.login.LoginManager;
import qingzhou.framework.util.Constants;
import qingzhou.framework.util.FileUtil;
import qingzhou.framework.util.StringUtil;
import qingzhou.console.view.ViewManager;
import qingzhou.console.view.impl.JsonView;
import qingzhou.framework.pattern.Filter;
import qingzhou.framework.pattern.FilterPattern;

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
    public static final String INDEX_PATH = REST_PREFIX + "/" + ViewManager.htmlView + "/" + Constants.MODEL_NAME_node + "/" + qingzhou.framework.api.Constants.LOCAL_NODE_NAME + "/" + qingzhou.framework.api.Constants.MASTER_APP_NAME + "/" + Constants.MODEL_NAME_index + "/" + Constants.ACTION_NAME_INDEX;
    public static final String MSG_FLAG = "MSG_FLAG";
    public static final File TEMP_BASE_PATH = ConsoleWarHelper.getCache();

    static {
        ConsoleContext master = ConsoleUtil.getAppContext(null).getConsoleContext();
        // 一些 filter 需要 i18n，如 LoginFreeFilter 调用了Helper.convertCommonMsg(msg)，此时 RestController 等类可能都还没有初始化（例如 Rest 直连登录），会导致 i18n 信息丢失，因此放到这里
        master.addI18N("validator.notexist", new String[]{"%s不存在", "en:%s does not exist"});// The product uses untrusted data to generated HTML pages. 客户端传来的参数不能回显到页面上，以阻止可能的xss攻击
        master.addI18N("validator.ActionEffective.notsupported", new String[]{"不支持%s操作，未满足条件：%s", "en:The %s operation is not supported, the condition is not met: %s"});// The product uses untrusted data to generated HTML pages. 客户端传来的参数不能回显到页面上，以阻止可能的xss攻击
        master.addI18N("batch.ops.success", new String[]{"%s%s成功%s个", "en:%s %s Success %s"});
        master.addI18N("batch.ops.fail", new String[]{"%s%s成功%s个，失败%s个，失败详情：", "en:%s%s success %s, failure %s, failure details:"});
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

    public static List<String> retrieveRestPathInfo(String uri) {
        List<String> result = new ArrayList<>();
        String[] restTemp = null;
        if (uri != null) {
            if (uri.startsWith("/")) {
                restTemp = uri.substring(1).split("/");
            } else {
                restTemp = uri.split("/");
            }
        }
        if (restTemp != null) {
            for (String r : restTemp) {
                if (r != null && !r.isEmpty()) {
                    result.add(r);
                }
            }
        }

        return result;
    }

    private final Filter<RestContext>[] filters = new Filter[]{
            new AccessControl(),
            new SearchFilter(),
            new AsymmetricFilter(),// 解密前端的 password 类型的表单域
            new InvokeAction() // 执行具体的业务逻辑
    };
    private final ViewManager viewManager = new ViewManager();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
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
        int restDepth = 6;
        if (rest.size() < restDepth) { // must have model & action
            String msg = "Parameters missing, make sure to use the correct REST interface: " + req.getRequestURI();
            resp.getWriter().print(JsonView.buildErrorResponse(msg));
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return null;
        }

        RequestImpl request = new RequestImpl();
        request.setViewName(rest.get(0));
        request.setTargetType(rest.get(1));
        request.setTargetName(rest.get(2));
        request.setAppName(rest.get(3));
        request.setModelName(rest.get(4));
        request.setActionName(rest.get(5));
        request.setUserName(LoginManager.getLoginUser(req.getSession(false)));

        if (rest.size() > restDepth) {
            String id = rest.get(restDepth);
            request.setId(id);
        }
        boolean actionFound = false;
        ModelAction[] actions = ConsoleUtil.getModelManager(request.getAppName()).getModelActions(request.getModelName());
        if (actions != null) {
            for (ModelAction ma : actions) {
                if (ma.name().equals(request.getActionName())) {
                    actionFound = true;
                    break;
                }
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
                data.put(k, String.join(qingzhou.framework.api.Constants.DATA_SEPARATOR, v));
            }
        }
        request.setParameters(data);
        if (fileAttachments != null) {
            // todo
//                requestImpl.setFileAttachments(fileAttachments);
//                for (Map.Entry<String, String> entry : fileAttachments.entrySet()) {
//                    if (StringUtil.notBlank(entry.getValue())) {
//                        int i = names.indexOf(entry.getKey());
//                        if (i == -1) {
//                            names.add(entry.getKey());
//                            vals.add(entry.getValue());
//                        } else {
//                            names.set(i, entry.getValue());
//                        }
//                    }
//                }
        }

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
