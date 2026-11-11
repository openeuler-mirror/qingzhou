package qingzhou.api.annotations;

import java.lang.annotation.*;

/**
 * {@link I18n} 的容器，由编译器在多次使用 {@code @I18n} 时合成，勿手工书写。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface I18ns {
    I18n[] value();
}
