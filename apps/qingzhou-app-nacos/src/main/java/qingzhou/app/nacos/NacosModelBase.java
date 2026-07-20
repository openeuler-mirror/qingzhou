package qingzhou.app.nacos;

import qingzhou.api.ModelBase;

/**
 * Nacos 模型基类
 * 提供统一的 Nacos API 访问、验证和错误处理
 */
public abstract class NacosModelBase extends ModelBase {
    
    /**
     * 获取 NacosApp 实例
     */
    protected NacosApp getNacosApp() {
        return getAppContext().getObjectInstance(NacosApp.class);
    }
    
    /**
     * 获取 NacosApi 实例
     */
    protected NacosApi getNacosApi() {
        NacosApp nacosApp = getNacosApp();
        if (nacosApp == null) {
            throw new IllegalStateException(NacosConstants.ERROR_NACOS_NOT_CONNECTED);
        }
        return nacosApp.getNacosApi();
    }
    
    // ==================== 错误处理方法 ====================
    
    /**
     * 处理操作失败
     */
    protected void handleFailure(String operation, String errorMessage) throws Exception {
        throw new Exception(operation + "失败: " + errorMessage);
    }
    
    /**
     * 处理操作失败（带原因）
     */
    protected void handleFailure(String operation, Exception cause) throws Exception {
        throw new Exception(operation + "失败: " + cause.getMessage(), cause);
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 获取默认值（如果为空）
     */
    protected String getOrDefault(String value, String defaultValue) {
        return value != null && !value.trim().isEmpty() ? value.trim() : defaultValue;
    }
    
    /**
     * 检查是否为 public 命名空间
     */
    protected boolean isPublicNamespace(String namespaceId) {
        return namespaceId == null || namespaceId.isEmpty() || 
               NacosConstants.PUBLIC_NAMESPACE.equals(namespaceId);
    }
    
    /**
     * 获取有效的命名空间 ID
     */
    protected String getEffectiveNamespaceId(String namespaceId) {
        return isPublicNamespace(namespaceId) ? NacosConstants.EMPTY_NAMESPACE : namespaceId;
    }
}