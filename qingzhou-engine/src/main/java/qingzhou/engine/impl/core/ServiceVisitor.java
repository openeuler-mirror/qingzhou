package qingzhou.engine.impl.core;

import java.util.Collection;

/**
 * 由引擎提供，开放给单个模块，供单个模块查看其它模块的服务注册信息
 */
public interface ServiceVisitor {
    Collection<Class<?>> allAppSharedServices();
}
