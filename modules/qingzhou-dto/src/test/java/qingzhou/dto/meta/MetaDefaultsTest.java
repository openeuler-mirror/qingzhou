package qingzhou.dto.meta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 护栏：annotation.json 只记录非默认值，缺省字段依赖 DTO 字段初值补齐，
 * 因此 DTO 初值必须与注解声明的默认值严格一致，避免两侧漂移导致元数据静默失真。
 */
public class MetaDefaultsTest {
    @Test
    public void dtoDefaultsMatchAnnotationDefaults() throws Exception {
        assertDefaults(new App(), qingzhou.api.annotations.App.class);
        assertDefaults(new Model(), qingzhou.api.annotations.Model.class);
        assertDefaults(new ModelAction(), qingzhou.api.annotations.ModelAction.class);
        assertDefaults(new ModelField(), qingzhou.api.annotations.ModelField.class);
        assertDefaults(new Menu(), qingzhou.api.annotations.Menu.class);
        assertDefaults(new I18n(), qingzhou.api.annotations.I18n.class);
    }

    private static void assertDefaults(Object dto, Class<?> annotationType) throws Exception {
        for (Field field : dto.getClass().getFields()) {
            Method attribute;
            try {
                attribute = annotationType.getMethod(field.getName());
            } catch (NoSuchMethodException e) {
                continue; // DTO 扩展字段（className、methodName、default_value 等）无对应注解属性
            }
            Object defaultValue = attribute.getDefaultValue();
            if (defaultValue == null) continue; // 必填属性无默认值，构建期必然写入
            Assert.assertTrue(Objects.deepEquals(defaultValue, field.get(dto)),
                    annotationType.getSimpleName() + "." + field.getName() + " 的 DTO 初值与注解默认值不一致");
        }
    }
}
