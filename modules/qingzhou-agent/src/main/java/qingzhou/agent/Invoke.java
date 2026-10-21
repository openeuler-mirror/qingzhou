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
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AgentDomainKeys;
import qingzhou.registry.AppStubLocal;
import qingzhou.registry.Registry;

@Component(property = {HttpHandler.HANDLE_PATH + "=" + Constants.REMOTE_INVOKE_URI, HttpHandler.HANDLE_NO_AUTH + "=true", HttpHandler.HANDLE_METHODS + "=POST"},
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
    public void setCrypto(Crypto crypto) {
        this.crypto = crypto;
    }

    @Reference
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    protected String aadName() {
        return Constants.REMOTE_INVOKE_URI;
    }

    @Override
    protected String doProcess(byte[] requestData, HttpRequest httpRequest) throws Throwable {
        RequestImpl request = json.fromJson(new String(requestData, StandardCharsets.UTF_8), RequestImpl.class);
        Set<File> uploadFiles = new HashSet<>(); // 待清理文件：上传原始临时文件 + rename 后的业务文件，Action 返回后统一删除
        try {
            request.getUploadFileFields().forEach(field -> {
                String[] keyNames = request.getParameter(field).split(Constants.REMOTE_UPLOAD_MULTIPLE_FILE_SP);
                List<String> paths = new ArrayList<>();
                for (String keyName : keyNames) {
                    String[] pair = keyName.split(Constants.REMOTE_UPLOAD_TEMP_KEY_FILE_SP);
                    if (pair.length < 2) { // 业务已有文件名（非本次上传项），原样保留交由应用处理
                        paths.add(keyName);
                        continue;
                    }
                    File tempKey = upload.resolveInUploadBase(pair[0]);
                    File tempFile = upload.resolveInUploadBase(pair[1]);
                    uploadFiles.add(tempKey);
                    if (!tempKey.renameTo(tempFile)) { // 重命名失败仍继续会向业务暴露不存在的路径
                        throw new IllegalStateException("failed to rename uploaded temp file: " + tempKey.getName());
                    }
                    uploadFiles.add(tempFile); // 业务用完即删：Action 返回后在 finally 删除；需长期保留应由 Action 内 move 到业务目录
                    paths.add(tempFile.getAbsolutePath());
                }
                String newPaths = String.join(",", paths); // TODO 应使用 ModelField 的 separator
                request.getParameters().put(field, newPaths);
            });

            AppStubLocal appStub = registry.getLocalApp(request.getApp());
            if (appStub == null) throw new IllegalArgumentException("business processing error");
            appStub.invokeApp(request);
        } finally {
            // 正常/异常路径均执行：原始临时文件 rename 后已不存在，delete 为空操作；
            // 业务文件用完即删，业务已在 Action 内 move 到自身目录时同样为空操作
            for (File file : uploadFiles) {
                if (!file.delete() && file.exists()) {
                    logger.warn("failed to delete upload file: " + file.getAbsolutePath());
                }
            }
        }

        // 响应数据
        return json.toJson(request.getResponse());
    }
}
