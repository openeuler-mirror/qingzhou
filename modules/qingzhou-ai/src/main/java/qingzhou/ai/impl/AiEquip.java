package qingzhou.ai.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import qingzhou.ai.AiSkill;
import qingzhou.api.Constants;
import qingzhou.dto.I18nService;
import qingzhou.http.server.HttpHandler;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.llm.ChatModelFactory;
import qingzhou.llm.Skill;

@Component(property = HttpHandler.HANDLE_PATH + "=/equip",
        service = {AiEquip.class, HttpHandler.class})
public class AiEquip implements HttpHandler {
    @Reference
    private ChatModelFactory chatModelFactory; // 作用：利用 OSGI DS 机制，迫使本模块在没有加载 llm 的情况下不要初始化。

    @Reference
    private Json json;
    @Reference
    private I18nService i18nService;

    final Map<AiSkill, Skill> llmSkills = new ConcurrentHashMap<>();

    private final List<String[]> promptsI18n = new ArrayList<String[]>() {{
        add(new String[]{"请概述轻舟平台的价值和意义", "en:Please summarize the value and significance of the Qingzhou platform"});
        add(new String[]{"请帮我查询轻舟平台上部署了哪些应用", "en:Please check what applications are deployed on the Qingzhou platform"});
        add(new String[]{"请问“图书管理”系统有多少读者数据？", "en:Could you tell me how many reader records are in the \"Book Management\" system?"});
        add(new String[]{"请为我展示“示例应用”中S001学生的信息", "en:Please show me the information of student S001 in the \"Demo Application\"."});
        add(new String[]{"如何开发一个轻舟应用？", "en:How to develop a Qingzhou application?"});
        add(new String[]{"以 nginx 为例说明如何实现轻舟的前后端分离部署", "en:Explain how to implement separated frontend and backend deployment for Qingzhou using Nginx as an example."});
        add(new String[]{"汇总“JVM”模块的健康状况", "en:Summarize the health status of the “JVM” module."});
    }};

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        Map<String, Object> data = new HashMap<>();

        String lang = httpRequest.getParameter(Constants.REQUEST_PARAMETER_NAME_LANG);
        List<String> prompts = promptsI18n.stream().map(i18n -> i18nService.getI18n(i18n, lang)).collect(Collectors.toList());
        data.put("prompts", prompts);

        List<Map<String, Object>> skills = new ArrayList<>();
        for (Map.Entry<AiSkill, Skill> entry : llmSkills.entrySet()) {
            AiSkill aiSkill = entry.getKey();
            Skill skill = entry.getValue();
            Map<String, Object> map = new HashMap<>();
            map.put("name", skill.name());
            map.put("text", i18nService.getI18n(aiSkill.getI18n(), lang));
            map.put("checked", true);
            skills.add(map);
        }
        data.put("skills", skills);

        String jsonData = json.toJson(data);
        httpResponse.contentTypeJsonUtf8()
                .sendFinish(jsonData);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void bindAiSkill(AiSkill skill, Map<String, Object> properties) {
        llmSkills.put(skill,
                Skill.of((String) properties.get(AiSkill.SKILL_NAME),
                        skill.description(),
                        skill.getInstruction(),
                        Converter.convertAiTool(skill.getTools()))
        );
    }

    // OSGI 框架根据名称规则自动识别调用此方法
    public void unbindAiSkill(AiSkill skill) {
        llmSkills.remove(skill);
    }
}
