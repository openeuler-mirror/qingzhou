package qingzhou.api.console;

import qingzhou.api.console.model.ListModel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Model {
    String name();

    String icon();

    String[] nameI18n();

    String[] infoI18n();

    String entryAction() default ListModel.ACTION_NAME_LIST;

    String menuName() default "";

    int menuOrder() default 0;
}
