package qingzhou.ai.impl;

import java.util.*;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import qingzhou.dto.Constants;
import qingzhou.dto.I18nService;
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
    @Reference
    private I18nService i18nService;

    private final List<String[]> promptSamples = Collections.unmodifiableList(Arrays.asList(
            new String[]{"请概述轻舟平台的价值和意义", "en:Please summarize the value and significance of the Qingzhou platform"},
            new String[]{"请帮我查询轻舟平台上部署了哪些应用", "en:Please check what applications are deployed on the Qingzhou platform"},
            new String[]{"请问“图书管理”系统有多少读者数据？", "en:Could you tell me how many reader records are in the \"Book Management\" system?"},
            new String[]{"请为我展示“示例应用”中S001学生的信息", "en:Please show me the information of student S001 in the \"Demo Application\"."},
            new String[]{"如何开发一个轻舟应用？", "en:How to develop a Qingzhou application?"},
            new String[]{"以 nginx 为例说明如何实现轻舟的前后端分离部署", "en:Explain how to implement separated frontend and backend deployment for Qingzhou using Nginx as an example."},
            new String[]{"汇总“JVM”模块的健康状况", "en:Summarize the health status of the “JVM” module."}));

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        Map<String, Object> data = new HashMap<>();

        String lang = httpRequest.getParameter(Constants.REQUEST_PARAMETER_NAME_LANG);
        List<String> prompts = promptSamples.stream().map(i18n -> i18nService.getI18n(i18n, lang)).collect(Collectors.toList());
        data.put("prompts", prompts);

        Map<String, Object> attachments = new HashMap<>();
        attachments.put("maxFiles", MAX_ATTACHMENT_FILES);
        attachments.put("maxFileSize", MAX_ATTACHMENT_MB);
        data.put("attachments", attachments);

        String jsonData = json.toJson(data);
        httpResponse.contentTypeJsonUtf8()
                .send(jsonData);
    }
}
