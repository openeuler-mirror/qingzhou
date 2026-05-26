package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface ModelField {
    String code();
    InputType input_type() default InputType.text;
    FieldType field_type() default FieldType.FORM;
    String display() default "";
    String ref_model() default "";
    String default_value() default "";
    String placeholder() default "";
    String[] options() default {};
    String separator() default ",";
    boolean id() default false;
    boolean readonly() default false;
    boolean required() default false;
    long min() default 0;
    long max() default 0;
    int min_length() default 0;
    int max_length() default -1;
    boolean host() default false;
    boolean port() default false;
    boolean email() default false;
    boolean file() default false;
    String pattern() default "";
    boolean add() default true;
    boolean update() default true;
    boolean show() default true;
    boolean list() default true;
    boolean search() default true;
    boolean numeric() default false;
    ChartType chart_type() default ChartType.line;
    String[] group() default {};
    String[] color() default {};
    int width_percent() default -1;
    String[] name();
    String[] info();
}