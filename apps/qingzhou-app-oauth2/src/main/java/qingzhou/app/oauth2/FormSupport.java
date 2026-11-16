package qingzhou.app.oauth2;

import java.util.LinkedHashMap;
import java.util.Map;

import qingzhou.api.Request;

/**
 * 表单动作的公共适配：只收集模型字段，并把业务校验失败转成表单可读错误。
 */
final class FormSupport {
    private FormSupport() {
    }

    static Map<String, String> collect(Request request, String[] fields) {
        Map<String, String> data = new LinkedHashMap<>();
        for (String field : fields) {
            String value = request.getParameter(field);
            if (value != null) data.put(field, value);
        }
        return data;
    }

    static void save(Request request, Operation operation) throws Exception {
        try {
            operation.run();
            request.getResponse().success(true).message("保存成功");
        } catch (IllegalArgumentException e) {
            request.getResponse().error(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    interface Operation {
        void run() throws Exception;
    }
}
