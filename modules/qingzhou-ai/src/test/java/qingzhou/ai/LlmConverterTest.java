package qingzhou.ai;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.llm.Skill;
import qingzhou.llm.Tool;

public class LlmConverterTest {
    @Test
    public void rolesProvided_toolInvoke_rolesReachToolService() throws Throwable {
        StubToolService toolService = new StubToolService();
        Tool tool = LlmConverter.convertTool(toolService, toolProp(), new String[]{"admin"});

        tool.invoke(Collections.<String, Object>emptyMap());

        Assert.assertEquals(toolService.roles.length, 1);
        Assert.assertEquals(toolService.roles[0], "admin");
    }

    @Test
    public void rolesProvided_skillConvert_invokeReachesToolService() throws Throwable {
        StubToolService toolService = new StubToolService();
        Skill skill = LlmConverter.convertSkill(new StubSkillService(toolService), skillProp(), new String[]{"admin", "reader"});

        Assert.assertEquals(skill.tools().size(), 1);
        skill.tools().iterator().next().invoke(Collections.<String, Object>emptyMap());

        Assert.assertEquals(toolService.roles.length, 2);
        Assert.assertEquals(toolService.roles[0], "admin");
        Assert.assertEquals(toolService.roles[1], "reader");
    }

    @Test
    public void rolesAbsent_toolInvoke_toolServiceReceivesNull() throws Throwable {
        StubToolService toolService = new StubToolService();
        Tool tool = LlmConverter.convertTool(toolService, toolProp(), null);

        tool.invoke(Collections.<String, Object>emptyMap());

        Assert.assertNull(toolService.roles);
    }

    private static Map<String, Object> toolProp() {
        Map<String, Object> prop = new HashMap<>();
        prop.put(ToolService.TOOL_NAME, "app_action_page");
        prop.put(ToolService.TOOL_DESCRIPTION, "show data list");
        return prop;
    }

    private static Map<String, Object> skillProp() {
        Map<String, Object> prop = new HashMap<>();
        prop.put(SkillService.SKILL_NAME, SkillService.SYSTEM_SKILL);
        return prop;
    }

    private static final class StubToolService implements ToolService {
        private String[] roles;

        @Override
        public String invoke(Map<String, Object> toolArgs) {
            return "invoked";
        }

        @Override
        public String invoke(Map<String, Object> toolArgs, String[] roles) {
            this.roles = roles;
            return "invoked";
        }
    }

    private static final class StubSkillService implements SkillService {
        private final ToolService toolService;

        StubSkillService(ToolService toolService) {
            this.toolService = toolService;
        }

        @Override
        public String[] nameI18n() {
            return new String[]{"system skill"};
        }

        @Override
        public String description() {
            return "system skill";
        }

        @Override
        public Map<ToolService, Map<String, Object>> tools() {
            return Collections.singletonMap(toolService, toolProp());
        }
    }
}
