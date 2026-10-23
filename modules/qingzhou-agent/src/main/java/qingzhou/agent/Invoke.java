package qingzhou.agent;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.common.FileUtil;
import qingzhou.dto.RequestImpl;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AgentDomainKeys;
import qingzhou.registry.AppStubLocal;
import qingzhou.registry.Constants;
import qingzhou.registry.Registry;

@Component(property = {HttpHandler.HANDLE_PATH + "=" + qingzhou.registry.Constants.REMOTE_INVOKE_URI, HttpHandler.HANDLE_NO_AUTH + "=true", HttpHandler.HANDLE_METHODS + "=POST"},
        service = HttpHandler.class)
public class Invoke extends AbstractAgentHandler {
    @Reference
    private Json json;
    @Reference
    private Registry registry;
    @Reference
    private Upload upload;

    @Reference
    public void setAgentDomainKeys(AgentDomainKeys agentDomainKeys) {
        this.agentDomainKeys = agentDomainKeys;
    }

    @Reference
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    protected String aadName() {
        return qingzhou.registry.Constants.REMOTE_INVOKE_URI;
    }

    @Override
    protected String doProcess(byte[] requestData, HttpRequest httpRequest) throws Throwable {
        if (requestData.length == 0) {
            throw new IllegalArgumentException("empty request");
        }
        RequestImpl request = json.fromJson(new String(requestData, StandardCharsets.UTF_8), RequestImpl.class);
        AppStubLocal appStub = registry.getLocalApp(request.getApp());
        if (appStub == null) {
            throw new IllegalArgumentException("app not found"); // 不回显 app 编码：消息经 AbstractAgentHandler 入日志
        }

        List<File> uploadFiles = new ArrayList<>();
        try {
            for (String field : request.getUploadFileFields()) {
                String value = request.getParameter(field);
                if (value == null) continue;

                List<String> paths = new ArrayList<>();
                for (String item : value.split(Constants.REMOTE_UPLOAD_MULTIPLE_FILE_SP)) {
                    String[] pair = item.split(qingzhou.registry.Constants.REMOTE_UPLOAD_TEMP_KEY_FILE_SP, 2);
                    if (pair.length < 2) { // 业务已有文件名（非本次上传项），原样保留交由应用处理
                        paths.add(item);
                        continue;
                    }

                    // 每次上传独占目录：落位到原文件名，同名文件之间互不覆盖
                    File dir = upload.uploadDir(pair[0]);
                    uploadFiles.add(dir); // 业务用完即删：需长期保留应由 Action 内 move 到业务目录
                    File source = upload.dataFile(dir);
                    File target = targetFile(dir, pair[1]);
                    FileUtil.moveFileOrDirectory(source, target);
                    paths.add(target.getAbsolutePath());
                }
                String newPaths = String.join(",", paths);
                request.getParameters().put(field, newPaths);
            }

            appStub.invokeApp(request);
        } finally {
            uploadFiles.forEach(FileUtil::forceDeleteQuietly);
        }

        return json.toJson(request.getResponse());
    }

    /**
     * 业务落位路径：只取文件名的末段，剥离可能携带的路径；同名冲突已由独占目录隔离。
     */
    private File targetFile(File uploadDir, String fileName) {
        String name = new File(fileName).getName();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) {
            throw new IllegalArgumentException("illegal upload file name: " + fileName);
        }
        return new File(uploadDir, name);
    }
}
