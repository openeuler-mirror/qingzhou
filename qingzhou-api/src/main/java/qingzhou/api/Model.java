package qingzhou.api;

import qingzhou.api.type.Listable;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Model {
    String name();

    String icon();

    String[] nameI18n();

    String[] infoI18n();

    String entryAction() default Listable.ACTION_NAME_LIST;

    boolean showToMenu() default true;

    String menuName() default "";

    int menuOrder() default 0;
}
