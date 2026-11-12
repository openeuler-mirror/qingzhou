package qingzhou.app.nacos;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import qingzhou.api.ModelBase;
import qingzhou.logger.Logger;

/**
 * Nacos 模型基类：收敛各模型共用的错误处理、id 解码与列表行投影，
 * 子类只保留与 Nacos API 的数据转换逻辑。
 */
public abstract class NacosModelBase extends ModelBase {

    protected NacosApi getNacosApi() {
        return NacosApp.nacosApi;
    }

    /**
     * 执行写操作：API 返回 false 或抛异常时统一按错误码上报。
     * 各模型 add/update/delete 的同一 try-catch 骨架收敛于此。
     */
    protected void writeOrFail(String errCode, String failMessage, WriteAction action) throws Exception {
        try {
            if (!action.run()) handleFailure(errCode, failMessage);
        } catch (Exception e) {
            handleFailure(errCode, e);
        }
    }

    protected interface WriteAction {
        boolean run() throws Exception;
    }

    protected void handleFailure(String operation, String errorMessage) throws Exception {
        throw new Exception(operation + "失败: " + errorMessage);
    }

    protected void handleFailure(String operation, Exception cause) throws Exception {
        throw new Exception(operation + "失败: " + cause.getMessage(), cause);
    }

    /** 前端传入的 id 可能含 URL 编码字符（如 dataId 中的斜杠），统一解码。 */
    protected String decode(String id) {
        try {
            return URLDecoder.decode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /** 把查询结果按 listFields 投影为列表行。 */
    protected List<String[]> toRows(List<Map<String, String>> items, String[] listFields) {
        List<String[]> rows = new ArrayList<>(items.size());
        for (Map<String, String> item : items) {
            String[] row = new String[listFields.length];
            for (int i = 0; i < listFields.length; i++) row[i] = item.getOrDefault(listFields[i], "");
            rows.add(row);
        }
        return rows;
    }

    protected String getOrDefault(String value, String defaultValue) {
        return value != null && !value.trim().isEmpty() ? value.trim() : defaultValue;
    }

    protected boolean isPublicNamespace(String namespaceId) {
        return namespaceId == null || namespaceId.isEmpty() || NacosConstants.PUBLIC_NAMESPACE.equals(namespaceId);
    }

    protected String getEffectiveNamespaceId(String namespaceId) {
        return isPublicNamespace(namespaceId) ? NacosConstants.EMPTY_NAMESPACE : namespaceId;
    }

    protected void logError(String message, Throwable e) {
        // 契约适配：Logger 服务可能未注册，不可因此中断主流程
        Logger logger = getAppContext().getService(Logger.class);
        if (logger != null) logger.error(message, e);
    }
}
