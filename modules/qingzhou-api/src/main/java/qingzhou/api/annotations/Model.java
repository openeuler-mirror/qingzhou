package qingzhou.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明一个模块：既可以是承载一组记录的数据模型（配合 {@code qingzhou.api.action} 下的能力接口），
 * 也可以只提供一个配置表单或监视面板。
 * <p>
 * 模块类必须继承 {@code qingzhou.api.ModelBase}；{@code code} 是接口路径中的 model 段，
 * 一旦对外发布即为契约。模块具备哪些动作由它实现了哪些能力接口决定，本注解只描述展示与权限。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Model {
    /**
     * 模块标识，缺省为类简名，须全应用唯一。
     */
    String code() default "";

    /**
     * 可访问角色，逗号分隔，{@code *} 表示任意已认证用户；未声明则继承 {@code @App.roles}。
     */
    String roles() default "";

    /**
     * 界面图标的名称或素材标识。
     */
    String icon() default "";

    /**
     * 所属菜单 code，对应 {@link Menu#code()}；为空时模型作为根节点呈现。
     */
    String menu() default "";

    /**
     * 同菜单内的排序，越小越靠前。
     */
    int order() default 9;

    /**
     * 模块名称，国际化数组。
     */
    String[] name();

    /**
     * 模块说明，国际化数组。
     */
    String[] info() default {};
}
