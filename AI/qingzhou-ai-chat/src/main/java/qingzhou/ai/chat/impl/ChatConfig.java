package qingzhou.ai.chat.impl;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;

@Component(property = HttpHandler.HANDLE_PATH + "=/chat/config",
        service = {ChatConfig.class, HttpHandler.class})
public class ChatConfig implements HttpHandler {
    // 附件限额：与 ChatConfig 对前端的宣称一致（maxFiles=10、maxFileSize=10MB，字符上限 10M）
    static final int MAX_ATTACHMENT_FILES = 10;
    private static final int MAX_ATTACHMENT_MB = 10;
    static final int MAX_ATTACHMENT_CHARS = MAX_ATTACHMENT_MB * 1_000_000;

    @Reference
    private Json json;

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        Map<String, Object> data = new HashMap<>();

        Map<String, Object> attachments = new HashMap<>();
        attachments.put("maxFiles", MAX_ATTACHMENT_FILES);
        attachments.put("maxFileSize", MAX_ATTACHMENT_MB);
        data.put("attachments", attachments);

        String jsonData = json.toJson(data);
        httpResponse.contentTypeJsonUtf8()
                .send(jsonData);
    }
}
