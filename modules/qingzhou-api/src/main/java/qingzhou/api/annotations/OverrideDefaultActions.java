package qingzhou.api.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface OverrideDefaultActions {
    ModelAction[] value();
}
