package qingzhou.api.annotations;

import java.lang.annotation.*;

/**
 * {@link ModelAction} 的容器，由编译器在类上多次使用 {@code @ModelAction} 时合成，勿手工书写。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface OverrideDefaultActions {
    ModelAction[] value();
}
