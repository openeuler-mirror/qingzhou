package qingzhou.agent;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Crypto;
import qingzhou.dto.Constants;
import qingzhou.dto.RequestImpl;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppStubLocal;
import qingzhou.registry.Registry;

@Component(property = {HttpHandler.HANDLE_PATH + "=" + Constants.AGENT_INVOKE_URI, HttpHandler.HANDLE_NO_AUTH + "=true", HttpHandler.HANDLE_METHODS + "=POST"})
public class AgentInvoker implements HttpHandler {
    @Reference
    private Json json;
    @Reference
    private Registry registry;
    @Reference
    private Crypto crypto;
    @Reference
    private FileUpload fileUpload;
    @Reference
    private Logger logger;

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        // invoke 报文整体唯一，无需额外 AAD 绑定上下文
        ProcessHandler.handle(httpRequest, httpResponse, crypto, logger, null, this::processRequest);
    }

    private String processRequest(byte[] data) throws Throwable {
        RequestImpl request = json.fromJson(new String(data, StandardCharsets.UTF_8), RequestImpl.class);

        Set<File> tempFiles = new HashSet<>(); // 处理前保存的原始临时文件（uploadBase/keyName[0]），防止异常路径残留

        try {
            request.getUploadFileFields().forEach(field -> {
                String[] keyNames = request.getParameter(field).split(Constants.AGENT_UPLOAD_MULTIPLE_FILE_FIELD_SP);
                List<String> paths = new ArrayList<>();
                for (String keyName : keyNames) {
                    String[] pair = keyName.split(Constants.AGENT_UPLOAD_MULTIPLE_FILE_NAME_SP);
                    if (pair.length < 2) { // 业务已有文件名（非本次上传项），原样保留交由应用处理
                        paths.add(keyName);
                        continue;
                    }
                    File tempFile = resolveInUploadBase(pair[0]);
                    File localFile = resolveInUploadBase(pair[1]);
                    tempFiles.add(tempFile);
                    if (!tempFile.renameTo(localFile)) { // 重命名失败仍继续会向业务暴露不存在的路径
                        throw new IllegalStateException("failed to rename uploaded temp file: " + tempFile.getName());
                    }
                    paths.add(localFile.getAbsolutePath());
                }
                String newPaths = String.join(Constants.AGENT_UPLOAD_MULTIPLE_FILE_FIELD_SP, paths);
                request.getParameters().put(field, newPaths);
            });

            AppStubLocal appStub = registry.getLocalApp(request.getApp());
            if (appStub == null) throw new IllegalArgumentException("business processing error");
            appStub.invokeApp(request);
        } finally {
            // 仅清理原始临时文件；renameTo 成功后原文件已不存在，delete 为空操作，不会误删业务使用的 localFile
            for (File tempFile : tempFiles) tempFile.delete();
        }

        // 响应数据
        return json.toJson(request.getResponse());
    }

    private File resolveInUploadBase(String path) {
        return fileUpload.resolveInUploadBase(path);
    }
}
