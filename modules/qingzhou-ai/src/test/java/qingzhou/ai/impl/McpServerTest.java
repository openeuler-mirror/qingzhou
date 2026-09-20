package qingzhou.ai.impl;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import qingzhou.ai.SkillService;
import qingzhou.ai.ToolService;
import qingzhou.http.server.AuthResult;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.logger.Logger;

public class McpServerTest {
    private static final String TOOL_NAME = "app_action_page";

    private StubToolService toolService;
    private McpServer mcpServer;

    @BeforeMethod
    public void setUp() throws Exception {
        toolService = new StubToolService();
        mcpServer = new McpServer();
        setField(mcpServer, "json", new StubJson());
        setField(mcpServer, "logger", stub(Logger.class));
        mcpServer.bindAiSkill(new StubSkillService(toolService), skillProperties());
    }

    @Test
    public void rolesInRequestAttribute_toolCall_rolesReachToolService() throws IOException {
        mcpServer.handle(request(new String[]{"reader"}), response());

        Assert.assertEquals(toolService.roles.length, 1);
        Assert.assertEquals(toolService.roles[0], "reader");
    }

    @Test
    public void rolesAbsentInRequestAttribute_toolCall_toolServiceReceivesNull() throws IOException {
        mcpServer.handle(request(null), response());

        Assert.assertNull(toolService.roles);
    }

    private static HttpRequest request(String[] roles) {
        byte[] body = ("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\""
                + TOOL_NAME + "\",\"arguments\":{}}}").getBytes(StandardCharsets.UTF_8);
        return (HttpRequest) Proxy.newProxyInstance(
                McpServerTest.class.getClassLoader(),
                new Class<?>[]{HttpRequest.class},
                (proxy, method, args) -> {
                    if ("getAttribute".equals(method.getName())) {
                        return AuthResult.AUTH_ROLES_ATTRIBUTE.equals(args[0]) ? roles : null;
                    }
                    if ("getBody".equals(method.getName())) return body;
                    return defaultValue(proxy, method, args);
                });
    }

    private static HttpResponse response() {
        return (HttpResponse) Proxy.newProxyInstance(
                McpServerTest.class.getClassLoader(),
                new Class<?>[]{HttpResponse.class},
                (proxy, method, args) -> {
                    if (method.getReturnType() == HttpResponse.class) return proxy;
                    return defaultValue(proxy, method, args);
                });
    }

    private static Map<String, Object> skillProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(SkillService.SKILL_NAME, SkillService.SYSTEM_SKILL);
        return properties;
    }

    private static <T> T stub(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(
                McpServerTest.class.getClassLoader(),
                new Class<?>[]{type},
                McpServerTest::defaultValue));
    }

    private static Object defaultValue(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        if ("toString".equals(name)) return "stub";
        if ("hashCode".equals(name)) return System.identityHashCode(proxy);
        if ("equals".equals(name)) return args != null && args.length > 0 && proxy == args[0];

        Class<?> returnType = method.getReturnType();
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        return null;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
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
            Map<String, Object> toolProp = new HashMap<>();
            toolProp.put(ToolService.TOOL_NAME, TOOL_NAME);
            toolProp.put(ToolService.TOOL_DESCRIPTION, "show data list");
            return Collections.singletonMap(toolService, toolProp);
        }
    }

    private static final class StubJson implements Json {
        @Override
        public String toJson(Object src) {
            return "{}";
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T fromJson(String json, Class<T> classOfT) {
            Map<String, Object> params = new HashMap<>();
            params.put("name", TOOL_NAME);
            params.put("arguments", new HashMap<String, Object>());

            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("jsonrpc", "2.0");
            requestMap.put("id", 1);
            requestMap.put("method", "tools/call");
            requestMap.put("params", params);
            return (T) requestMap;
        }
    }
}
