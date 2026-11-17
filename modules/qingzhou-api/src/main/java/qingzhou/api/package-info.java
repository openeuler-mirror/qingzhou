/**
 * 轻舟应用的公共 SPI：模块基类、请求/响应、运行期上下文与过滤器链。
 * <p>
 * 应用以普通 jar 部署，构建期由平台提取注解元数据，运行期转换为 OSGi bundle 后由框架反射装配：
 * 先创建 {@link qingzhou.api.QingzhouApp} 实例，再为每个模块创建 {@link qingzhou.api.ModelBase} 子类实例
 * 并注入 {@link qingzhou.api.AppContext}，最后依次回调 start。
 * <p>
 * 本包中的接口由平台实现并传入，应用侧只实现 {@link qingzhou.api.QingzhouApp}、
 * {@link qingzhou.api.ModelBase} 与 {@code qingzhou.api.action} 下的能力接口。
 */
package qingzhou.api;
