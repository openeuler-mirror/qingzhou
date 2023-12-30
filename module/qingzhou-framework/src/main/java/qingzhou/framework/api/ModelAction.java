package qingzhou.framework.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelAction {
    String name();

    String icon();

    String[] nameI18n();

    String[] infoI18n();

    String effectiveWhen() default "";

    String forwardToPage() default "";
}
