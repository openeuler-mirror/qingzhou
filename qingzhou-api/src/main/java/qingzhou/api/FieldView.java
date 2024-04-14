package qingzhou.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldView {
    /**
     * 设置字段所属的页面表单展示分组，默认为空字符串表示无分组。
     *
     * @return 字段所属的组名
     */
    String group() default "";

    /**
     * 设置字段的数据类型，默认为文本类型（FieldType.text）。
     *
     * @return 字段的数据类型
     */
    FieldType type() default FieldType.text;
}
