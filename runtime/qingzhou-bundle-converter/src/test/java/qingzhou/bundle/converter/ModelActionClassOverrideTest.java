package qingzhou.bundle.converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import qingzhou.api.action.Add;
import qingzhou.api.annotations.Model;
import qingzhou.api.annotations.ModelAction;
import qingzhou.api.annotations.ModelActions;

public class ModelActionClassOverrideTest {

    private BundleConverter converter;
    private Method mergeMethod;
    private Method applyMethod;
    private ClassPool classPool;

    @BeforeMethod
    public void setUp() throws Exception {
        converter = new BundleConverter();
        mergeMethod = BundleConverter.class.getDeclaredMethod("mergeObjAnnotation", Object.class, Annotation.class);
        mergeMethod.setAccessible(true);
        applyMethod = BundleConverter.class.getDeclaredMethod("applyClassAction", ModelAction.class, Set.class);
        applyMethod.setAccessible(true);
        classPool = ClassPool.getDefault();
    }

    // 运行时 ModelAction 为 CLASS 保留策略，需经 javassist 从字节码读取（与 BundleConverter 路径一致）
    private ModelAction classAnno(Class<?> clazz) throws Exception {
        CtClass ctClass = classPool.get(clazz.getName());
        return (ModelAction) ctClass.getAnnotation(ModelAction.class);
    }

    private ModelActions classContainer(Class<?> clazz) throws Exception {
        CtClass ctClass = classPool.get(clazz.getName());
        return (ModelActions) ctClass.getAnnotation(ModelActions.class);
    }

    @Model(code = "test", name = {"t"})
    @ModelAction(code = "add", icon = "UserPlus")
    static class AnnoSource implements Add {
        public void add(java.util.Map<String, String> data) {
        }
    }

    @Test
    public void mergeObjAnnotation_overridesOnlyNonDefaultFields() throws Exception {
        qingzhou.dto.meta.ModelAction dto = new qingzhou.dto.meta.ModelAction();
        dto.isDefaultAction = true;
        dto.code = "add";
        dto.icon = "Check";
        dto.order = 1;
        dto.name = new String[]{"原始"};
        dto.info = new String[]{"原始描述"};

        ModelAction anno = classAnno(AnnoSource.class);
        Assert.assertNotNull(anno);
        mergeMethod.invoke(converter, dto, anno);

        Assert.assertEquals(dto.icon, "UserPlus", "icon 应被覆盖");
        Assert.assertEquals(dto.order, 1, "order 未显式声明应保持原值");
        Assert.assertEquals(dto.name, new String[]{"原始"}, "name 未显式声明应保持原值");
        Assert.assertEquals(dto.info, new String[]{"原始描述"}, "info 未显式声明应保持原值");
    }

    @Model(code = "test", name = {"t"})
    @ModelAction(code = "add", icon = "Star", name = {"新增"})
    static class ClassAnnoAdd implements Add {
        public void add(java.util.Map<String, String> data) {
        }
    }

    @Model(code = "test", name = {"t"})
    @ModelAction(code = "delete", icon = "Trash2", confirm = {"确认删除？"})
    static class ClassAnnoDelete implements Add {
        public void add(java.util.Map<String, String> data) {
        }
    }

    @Model(code = "test", name = {"t"})
    @ModelAction(code = "", icon = "Invalid")
    static class ClassAnnoEmptyCode implements Add {
        public void add(java.util.Map<String, String> data) {
        }
    }

    @Test
    public void applyClassAction_matchingCode_mergesIntoDefaultAction() throws Exception {
        qingzhou.dto.meta.ModelAction dto = new qingzhou.dto.meta.ModelAction();
        dto.isDefaultAction = true;
        dto.code = "add";
        dto.icon = "Check";
        dto.order = 1;
        dto.name = new String[]{"保存"};

        ModelAction anno = classAnno(ClassAnnoAdd.class);
        applyMethod.invoke(converter, anno, java.util.Collections.singleton(dto));

        Assert.assertEquals(dto.icon, "Star");
        Assert.assertEquals(dto.name, new String[]{"新增"});
        Assert.assertEquals(dto.order, 1, "order 未显式声明应保持原值");
        Assert.assertTrue(dto.isDefaultAction, "合并后仍应为默认 action，执行逻辑不变");
    }

    @Test
    public void applyClassAction_nonMatchingCode_noEffect() throws Exception {
        qingzhou.dto.meta.ModelAction dto = new qingzhou.dto.meta.ModelAction();
        dto.isDefaultAction = true;
        dto.code = "add";
        dto.icon = "Check";

        ModelAction anno = classAnno(ClassAnnoDelete.class);
        applyMethod.invoke(converter, anno, java.util.Collections.singleton(dto));

        Assert.assertEquals(dto.icon, "Check", "code 不匹配不应修改");
    }

    @Test
    public void applyClassAction_emptyCode_skipped() throws Exception {
        qingzhou.dto.meta.ModelAction dto = new qingzhou.dto.meta.ModelAction();
        dto.isDefaultAction = true;
        dto.code = "add";
        dto.icon = "Check";

        ModelAction anno = classAnno(ClassAnnoEmptyCode.class);
        applyMethod.invoke(converter, anno, java.util.Collections.singleton(dto));

        Assert.assertEquals(dto.icon, "Check", "空 code 不应修改任何 DTO");
    }

    @Test
    public void applyClassAction_notDefaultAction_noEffect() throws Exception {
        qingzhou.dto.meta.ModelAction dto = new qingzhou.dto.meta.ModelAction();
        dto.isDefaultAction = false;
        dto.code = "add";
        dto.icon = "Check";

        ModelAction anno = classAnno(ClassAnnoAdd.class);
        applyMethod.invoke(converter, anno, java.util.Collections.singleton(dto));

        Assert.assertEquals(dto.icon, "Check", "非默认 action 不应被类级注解覆盖");
    }

    // —— 多个 @ModelAction 场景：编译器自动打包成 @ModelActions 容器 ——

    @Model(code = "test", name = {"t"})
    @ModelAction(code = "add",    icon = "Star",  name = {"新增"})
    @ModelAction(code = "delete", icon = "Trash2", confirm = {"确定删除？"})
    static class MultiClassAnno implements Add {
        public void add(java.util.Map<String, String> data) {
        }
    }

    @Test
    public void multiModelAction_compilerPacksIntoModelActions() throws Exception {
        // 编译器对多个 @ModelAction 自动打包成 @ModelActions 容器
        Assert.assertNull(classAnno(MultiClassAnno.class), "多注解场景，ModelAction.class 应返回 null");
        ModelActions container = classContainer(MultiClassAnno.class);
        Assert.assertNotNull(container, "多注解场景，应通过 ModelActions 容器获取");
        Assert.assertEquals(container.value().length, 2);
        Assert.assertEquals(container.value()[0].code(), "add");
        Assert.assertEquals(container.value()[1].code(), "delete");
    }

    @Test
    public void multiModelAction_eachMergesItsOwnDefaultAction() throws Exception {
        // 模拟 BundleConverter 实际流程：两个默认 action DTO + 一个非默认 action DTO
        qingzhou.dto.meta.ModelAction dtoAdd = new qingzhou.dto.meta.ModelAction();
        dtoAdd.isDefaultAction = true;
        dtoAdd.code = "add";
        dtoAdd.icon = "Check";
        dtoAdd.order = 1;

        qingzhou.dto.meta.ModelAction dtoDelete = new qingzhou.dto.meta.ModelAction();
        dtoDelete.isDefaultAction = true;
        dtoDelete.code = "delete";
        dtoDelete.icon = "Delete";
        dtoDelete.confirm = new String[]{"默认删除确认"};

        qingzhou.dto.meta.ModelAction dtoCustom = new qingzhou.dto.meta.ModelAction();
        dtoCustom.isDefaultAction = false;
        dtoCustom.code = "add"; // 自定义 action 同 code，应不受类级注解影响
        dtoCustom.icon = "CustomIcon";

        java.util.Set<qingzhou.dto.meta.ModelAction> dtos = new java.util.LinkedHashSet<>();
        dtos.add(dtoAdd);
        dtos.add(dtoDelete);
        dtos.add(dtoCustom);

        ModelActions container = classContainer(MultiClassAnno.class);
        Assert.assertNotNull(container);
        for (ModelAction a : container.value()) {
            applyMethod.invoke(converter, a, dtos);
        }

        // add 的 icon/name 被覆盖
        Assert.assertEquals(dtoAdd.icon, "Star");
        Assert.assertEquals(dtoAdd.name, new String[]{"新增"});
        Assert.assertEquals(dtoAdd.order, 1, "未声明字段保持原值");
        Assert.assertTrue(dtoAdd.isDefaultAction);

        // delete 的 icon/confirm 被覆盖
        Assert.assertEquals(dtoDelete.icon, "Trash2");
        Assert.assertEquals(dtoDelete.confirm, new String[]{"确定删除？"});
        Assert.assertTrue(dtoDelete.isDefaultAction);

        // 非默认 action 不受影响
        Assert.assertEquals(dtoCustom.icon, "CustomIcon", "自定义 action 不应被类级注解覆盖");
    }
}
