package qingzhou.api.annotations;

import java.lang.annotation.*;

/**
 * {@link Menu} 的容器，由编译器在多次使用 {@code @Menu} 时合成，勿手工书写。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Menus {
    Menu[] value();
}
