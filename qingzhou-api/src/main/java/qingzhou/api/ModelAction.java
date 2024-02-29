package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelAction {
    String name();

    String icon() default "";

    String[] nameI18n();

    String[] infoI18n();

    String effectiveWhen() default "";

    String forwardToPage() default "";

    boolean showToListHead() default false;

    boolean showToList() default false;

    int orderOnList() default 0;

    boolean supportBatch() default false;

    boolean disabled() default false;
}
