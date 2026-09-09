package qingzhou.agent;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.crypto.Crypto;
import qingzhou.dto.Constants;
import qingzhou.dto.RequestImpl;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.registry.AppStubLocal;
import qingzhou.registry.Registry;

@Component(property = HttpHandler.HANDLE_PATH + "=" + Constants.AGENT_INVOKE_URI)
public class AgentInvoker implements HttpHandler {
    @Reference
    private Json json;
    @Reference
    private Registry registry;
    @Reference
    private Crypto crypto;
    @Reference
    private FileUpload fileUpload;

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) {
        ProcessHandler.handle(httpRequest, httpResponse, crypto, this::processRequest);
    }

    private String processRequest(byte[] data) throws Throwable {
        // 3. 得到请求对象
        RequestImpl request = json.fromJson(new String(data, StandardCharsets.UTF_8), RequestImpl.class);

        // 4. 处理
        Set<String> originalFilePaths = new HashSet<>(); // 处理之前，须先保存原始文件路径，用于 finally 块中的清理，因应用可能修改此参数值
        Set<File> rawTempFiles = new HashSet<>(); // 处理前保存的原始临时文件（uploadBase/keyName[0]），防止异常路径残留
        final String fileSp = ","; // TODO：应引用 ModelField.separator()
        try {
            request.getUploadFileFields().forEach(field -> {
                String[] keyNames = request.getParameter(field).split(Constants.AGENT_UPLOAD_MULTIPLE_FILE_FIELD_SP);
                for (String keyName : keyNames) {
                    rawTempFiles.add(resolveInUploadBase(keyName.split(Constants.AGENT_UPLOAD_MULTIPLE_FILE_NAME_SP)[0]));
                }
                String newPaths = Arrays.stream(keyNames).map(s -> {
                    String[] keyName = s.split(Constants.AGENT_UPLOAD_MULTIPLE_FILE_NAME_SP);
                    File tempFile = resolveInUploadBase(keyName[0]);
                    File localFile = resolveInUploadBase(keyName[1]);
                    tempFile.renameTo(localFile);
                    return localFile.getAbsolutePath();
                }).collect(Collectors.joining(fileSp));
                request.getParameters().put(field, newPaths);
                originalFilePaths.add(newPaths);
            });

            AppStubLocal appStub = registry.getLocalApp(request.getApp());
            appStub.invokeApp(request);
        } finally {
            for (String paths : originalFilePaths) {
                for (String path : paths.split(fileSp)) {
                    try {
                        resolveInUploadBase(path.trim()).delete();
                    } catch (IllegalArgumentException e) {
                        // 路径越界，不删除
                    }
                }
            }
            for (File tempFile : rawTempFiles) {
                tempFile.delete(); // 已被 rename 的文件此处删除无副作用
            }
        }

        // 响应数据
        return json.toJson(request.getResponse());
    }

    private File resolveInUploadBase(String path) {
        return fileUpload.resolveInUploadBase(path);
    }
}
