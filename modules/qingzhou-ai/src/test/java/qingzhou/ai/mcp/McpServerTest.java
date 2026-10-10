package qingzhou.ai.mcp;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.ai.SkillService;
import qingzhou.ai.impl.InterceptToolInvocation;
import qingzhou.ai.ToolService;
import qingzhou.http.server.HttpRequest;
import qingzhou.http.server.HttpResponse;
import qingzhou.json.Json;
import qingzhou.json.impl.JsonImpl;
import qingzhou.logger.Logger;

public class McpServerTest {
    private static final String TOOL = "demo_tool";

    private static <T> T stub(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(McpServerTest.class.getClassLoader(),
                new Class<?>[]{type}, McpServerTest::defaultValue));
    }

    private static Object defaultValue(Object proxy, Method method, Object[] args) {
        if (method.getName().startsWith("is") && method.getReturnType() == boolean.class) return false;
        if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
        if ("equals".equals(method.getName())) return args != null && args.length > 0 && proxy == args[0];
        return null;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * 构造 McpServer：注入真实 Json、桩 Logger、可控 ToolInvoker，并注册一个 demo 工具
     */
    private McpServer newServer(boolean denyAll) throws Exception {
        McpServer server = new McpServer();

        JsonImpl json = new JsonImpl();
        json.init();
        setField(server, "json", json);
        setField(server, "logger", stub(Logger.class));

        InterceptToolInvocation invoker = new InterceptToolInvocation() {
            @Override
            public String intercept(String toolName, Map<String, Object> args, String principal, String[] roles) {
                return denyAll ? "no permission" : null;
            }
        };
        setField(server, "toolInvoker", invoker);

        // 注册一个技能，其下挂一个可返回结果或抛异常的 demo 工具
        SkillService skill = new SkillService() {
            @Override
            public Map<ToolService, Map<String, Object>> tools() {
                Map<String, Object> props = new HashMap<>();
                props.put(ToolService.TOOL_NAME, TOOL);
                props.put(ToolService.TOOL_DESCRIPTION, "demo");
                ToolService ts = new ToolService() {
                    @Override
                    public String invoke(Map<String, Object> toolArgs) {
                        if (toolArgs != null && "boom".equals(toolArgs.get("mode"))) {
                            throw new RuntimeException("internal secret detail");
                        }
                        return "ok";
                    }
                };
                return Collections.singletonMap(ts, props);
            }
        };
        Map<String, Object> skillProps = new HashMap<>();
        skillProps.put(SkillService.SKILL_NAME, "demo_skill");
        skillProps.put(SkillService.SKILL_DESCRIPTION, "demo skill");
        skillProps.put(SkillService.SKILL_REQUIRED, "true");

        @SuppressWarnings("unchecked")
        Map<SkillService, Map<String, Object>> llmSkills =
                (Map<SkillService, Map<String, Object>>) server.getClass()
                        .getDeclaredField("llmSkills").get(server);
        llmSkills.put(skill, skillProps);
        return server;
    }

    private static HttpRequest request(byte[] body, String principal, String[] roles) {
        return new HttpRequest() {
            @Override
            public byte[] getBody() {
                return body;
            }

            @Override
            public String getPrincipal() {
                return principal;
            }

            @Override
            public String[] getRoles() {
                return roles;
            }

            @Override
            public String getRemoteHost() {
                return "127.0.0.1";
            }

            @Override
            public String getPath() {
                return "/mcp";
            }

            @Override
            public String getRawUri() {
                return "/mcp";
            }

            @Override
            public String getParameter(String name) {
                return null;
            }

            @Override
            public Map<String, java.util.List<String>> getParameters() {
                return null;
            }

            @Override
            public String getHeader(String header) {
                return null;
            }

            @Override
            public String getContentType() {
                return "application/json";
            }

            @Override
            public String getMethod() {
                return "POST";
            }

            @Override
            public String getScheme() {
                return "http";
            }

            @Override
            public boolean isFormUrlencoded() {
                return false;
            }
        };
    }

    private static final class CapturingResponse implements HttpResponse {
        int status = 200;
        String body;

        @Override
        public HttpResponse status(int s) {
            status = s;
            return this;
        }

        @Override
        public HttpResponse header(String n, String v) {
            return this;
        }

        @Override
        public HttpResponse contentType(String v) {
            return this;
        }

        @Override
        public HttpResponse contentTypeJsonUtf8() {
            return this;
        }

        @Override
        public HttpResponse send(String b) {
            body = b;
            return this;
        }

        @Override
        public HttpResponse send(byte[] b) {
            body = new String(b);
            return this;
        }

        @Override
        public void finish() {
        }

        @Override
        public void redirect(String url) {
        }
    }

    private Map<String, Object> handle(McpServer server, Json json, HttpRequest req) throws Exception {
        CapturingResponse resp = new CapturingResponse();
        server.handle(req, resp);
        Assert.assertNotNull(resp.body, "响应体不应为空");
        return json.fromJson(resp.body, HashMap.class);
    }

    @Test
    public void unknownMethod_returnsMethodNotFoundError() throws Exception {
        McpServer server = newServer(false);
        JsonImpl json = new JsonImpl();
        json.init();
        String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"bogus\"}";
        Map<String, Object> resp = handle(server, json, request(body.getBytes(), "admin", new String[]{"admin"}));

        Assert.assertNull(resp.get("result"), "未知 method 不应返回 result");
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) resp.get("error");
        Assert.assertNotNull(error, "未知 method 应返回 error");
        Assert.assertEquals(error.get("code"), -32601);
    }

    @Test
    public void toolsCall_missingParams_returnsInvalidParams() throws Exception {
        McpServer server = newServer(false);
        JsonImpl json = new JsonImpl();
        json.init();
        String body = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\"}";
        Map<String, Object> resp = handle(server, json, request(body.getBytes(), "admin", new String[]{"admin"}));

        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) resp.get("error");
        Assert.assertNotNull(error);
        Assert.assertEquals(error.get("code"), -32602);
    }

    @Test
    public void toolsCall_authDenied_returnsErrorWithoutInvokingTool() throws Exception {
        McpServer server = newServer(true);
        JsonImpl json = new JsonImpl();
        json.init();
        String body = "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"" + TOOL + "\",\"arguments\":{}}}";
        Map<String, Object> resp = handle(server, json, request(body.getBytes(), "admin", new String[]{"admin"}));

        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) resp.get("error");
        Assert.assertNotNull(error, "授权拒绝应返回 error");
        Assert.assertEquals(error.get("message"), "no permission");
        Assert.assertNull(resp.get("result"), "授权拒绝时不得执行工具");
    }

    @Test
    public void toolsCall_toolThrows_exceptionMaskedFromClient() throws Exception {
        McpServer server = newServer(false);
        JsonImpl json = new JsonImpl();
        json.init();
        String body = "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"" + TOOL + "\",\"arguments\":{\"mode\":\"boom\"}}}";
        Map<String, Object> resp = handle(server, json, request(body.getBytes(), "admin", new String[]{"admin"}));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.get("result");
        Assert.assertNotNull(result, "工具抛异常不应丢失 result 信封");
        Assert.assertEquals(result.get("isError"), Boolean.TRUE);
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> content = (java.util.List<Map<String, Object>>) result.get("content");
        String text = (String) content.get(0).get("text");
        Assert.assertEquals(text, "tool invocation failed");
        Assert.assertFalse(text.contains("internal secret"), "异常内部信息不得回传客户端");
    }

    @Test
    public void toolsCall_allowed_returnsToolResult() throws Exception {
        McpServer server = newServer(false);
        JsonImpl json = new JsonImpl();
        json.init();
        String body = "{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"" + TOOL + "\",\"arguments\":{}}}";
        Map<String, Object> resp = handle(server, json, request(body.getBytes(), "admin", new String[]{"admin"}));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.get("result");
        Assert.assertNotNull(result);
        Assert.assertEquals(result.get("isError"), Boolean.FALSE);
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> content = (java.util.List<Map<String, Object>>) result.get("content");
        Assert.assertEquals(content.get(0).get("text"), "ok");
    }
}
