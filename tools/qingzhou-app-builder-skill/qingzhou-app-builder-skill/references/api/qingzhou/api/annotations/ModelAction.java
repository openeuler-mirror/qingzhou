package qingzhou.api.annotations;

import java.lang.annotation.*;

/**
 * 声明一个动作，标注位置不同则语义不同：
 * <ul>
 *   <li>标注在方法上：自定义动作。方法签名为 {@code (Request)}，执行期间
 *       {@link qingzhou.api.ModelBase#getCurrentRequest()} 有值。若 {@link #code()} 与内置动作同名，
 *       仅替换该内置动作的元数据，执行逻辑仍是框架实现（即调用模块实现的能力接口）。</li>
 *   <li>标注在类上：只覆盖内置动作的展示与权限元数据（图标、名称、排序、显示条件、确认文案等），
 *       不改动执行逻辑；{@link #code()} 必须对应模块已具备的内置动作，否则被忽略。</li>
 * </ul>
 * 内置动作由模块实现 {@code qingzhou.api.action} 下的能力接口自动生成，无需再写注解。
 * <p>
 * 除 {@link #roles()} 参与后端鉴权外，其余属性只影响界面渲染。
 */
@Repeatable(OverrideDefaultActions.class)
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface ModelAction {
    /**
     * 动作标识，即接口路径中的 action 段，缺省为方法名。
     */
    String code() default "";

    /**
     * 可访问角色，逗号分隔，{@code *} 表示任意已认证用户；未声明则继承 {@code @Model.roles}，
     * 再继承 {@code @App.roles}。
     */
    String roles() default "";

    /**
     * 按钮图标的名称或素材标识。
     */
    String icon() default "";

    /**
     * 按钮排序，越小越靠前。
     */
    int order() default 9;

    /**
     * 按钮的可用条件表达式，语法同 {@code @ModelField.display}。只控制界面显隐，不构成权限。
     */
    String display() default "";

    /**
     * 是否作为新增表单的提交入口。
     */
    boolean add() default false;

    /**
     * 是否作为更新表单的提交入口。
     */
    boolean update() default false;

    /**
     * 是否出现在查看/详情页。
     */
    boolean show() default false;

    /**
     * 是否出现在列表页工具栏（可配合 {@link #batch()} 形成批量操作）。
     */
    boolean list_head() default false;

    /**
     * 是否出现在列表的每一行。
     */
    boolean list() default false;

    /**
     * 是否按勾选行批量执行，此时前端以 {@code ids} 参数提交全部选中标识。
     */
    boolean batch() default false;

    /**
     * 动作名称，国际化数组。
     */
    String[] name() default {};

    /**
     * 动作说明，国际化数组。
     */
    String[] info() default {};

    /**
     * 确认提示，国际化数组。非空时界面在执行前弹出确认对话框。
     */
    String[] confirm() default {};

    /**
     * 标记执行结果以弹出层展示。
     */
    boolean popup() default false;
}
