package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义模型的操作。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelAction {
    // 提供一个国际化资源键数组，用于根据不同的语言环境加载相应的操作名称。
    String[] name();

    // 提供一个国际化资源键数组，用于根据不同的语言环境加载相应的操作详细信息。
    String[] info() default {};

    String code(); // 接口名

    String icon() default ""; // 指定与该模型操作关联的图标名称

    boolean request_body() default false; // 为 true 后，可通过 Request.getInputStream 得到 http 消息体

    boolean auth_free() default false; // 免登录可访问

    ActionType action_type() default ActionType.link;

    String display() default ""; // 设置该操作的可用条件

    boolean show_action() default false;

    boolean list_action() default false;

    boolean head_action() default false;

    boolean batch_action() default false;

    boolean form_action() default false;

    String order() default "5"; // 排序符，按自然顺序

    String[] sub_form_fields() default {}; // 在list页面上，弹出表单页面，指定表单页面里显示的字段

    boolean sub_form_autoload() default false;    // 列表上的弹出表单加载后触发提交，列表头上的不会触发

    boolean sub_form_autoclose() default false;

    String[] sub_menu_models() default {}; //  弹出子级管理页面

    String app_page() default ""; // 跳转到指定的页面，注：app根目录后的相对路径
}
