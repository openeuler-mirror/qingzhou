package qingzhou.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明应用元数据并绑定入口类：被标注的类必须实现 {@code qingzhou.api.QingzhouApp}，整个应用内只允许存在一个。
 * <p>
 * 构建期平台提取本注解（含该类名）落盘，运行期据此反射创建入口实例，因此本注解是应用被发现的前提。
 * {@code roles} 处于权限链最高层：模块与动作未声明时逐级继承至此，且"未声明即拒绝"，
 * 应用级至少要声明一次，否则所有请求返回 403。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface App {
    /**
     * 应用标识，参与接口地址与临时目录命名。缺省取 jar 名，实例配置中的 {@code code} 可覆盖，
     * 多实例部署时会追加序号后缀。
     */
    String code() default "";

    /**
     * 可访问角色，逗号分隔，{@code *} 表示任意已认证用户。
     */
    String roles() default "";

    /**
     * 界面图标的名称或素材标识。
     */
    String icon() default "";

    /**
     * 应用名称，国际化数组。
     */
    String[] name();

    /**
     * 应用说明，国际化数组。
     */
    String[] info() default {};
}
