package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface ModelAction {
    String code();
    String icon() default "";
    int order() default 0;
    String display() default "";
    String[] name();
    String[] info();
    boolean add() default false;
    boolean update() default false;
    boolean show() default true;
    boolean list() default true;
    boolean list_head() default false;
    boolean batch() default false;
}