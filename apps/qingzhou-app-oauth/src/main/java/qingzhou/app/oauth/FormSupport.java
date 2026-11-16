package qingzhou.app.oauth;

import qingzhou.api.Request;

/**
 * 表单动作的公共适配：只收集模型字段，并把业务校验失败转成表单可读错误。
 */
final class FormSupport {
    private FormSupport() {
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
