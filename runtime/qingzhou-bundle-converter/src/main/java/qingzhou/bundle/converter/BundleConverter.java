package qingzhou.bundle.converter;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javassist.*;
import org.osgi.framework.Constants;
import qingzhou.api.annotations.*;
import qingzhou.app.driver.DefaultAction;
import qingzhou.common.FileUtil;
import qingzhou.json.impl.JsonImpl;

public class BundleConverter {
    private ClassPool classPool;
    private String appDynamicPackages;

    public void build(File sourceJar, File targetJar, String libDir) throws Exception {
        // 待处理的原应用的 jar
        File appTmpDir = new File(targetJar.getParentFile(), targetJar.getName() + UUID.randomUUID());
        if (!appTmpDir.mkdirs() && !appTmpDir.isDirectory()) {
            throw new IllegalStateException("cannot create temp dir: " + appTmpDir);
        }
        try {
            // overwrite=true 覆盖同名 class（防应用遮蔽驱动实现）；false 跳过已存在项（保留应用侧自定义文件）
            FileUtil.unZipToDir(sourceJar, appTmpDir, false);

            // 生成注解文件
            if (classPool == null) {
                ClassPool.doPruning = true;
                classPool = ClassPool.getDefault();
                classPool.appendClassPath(new LoaderClassPath(this.getClass().getClassLoader()));
            }
            // 中途异常也须摘除 classPath，否则残留条目会让后续构建解析到已删除目录里的类
            ClassPath appendedClassPath = classPool.appendClassPath(appTmpDir.getAbsolutePath());
            try {
                addAnnotationFile(appTmpDir, targetJar);
            } finally {
                classPool.removeClassPath(appendedClassPath);
            }

            // 放入 OSGI 驱动类
            addDriverClasses(appTmpDir, libDir);

            // 添加 MANIFEST.MF 中 OSGI 声明
            addManifest(appTmpDir, targetJar);

            // 构建为 bundle jar
            File[] files = appTmpDir.listFiles();
            if (files != null) {
                FileUtil.zipFiles(files, targetJar, true);
            }
        } finally {
            // 无论成功或异常都清理临时目录，否则随 UUID 无限累积（磁盘耗尽）
            FileUtil.forceDeleteQuietly(appTmpDir);
        }
    }

    private void addManifest(File appTmpDir, File targetJar) throws Exception {
        Manifest manifest;
        Path manifestPath = Paths.get(appTmpDir.getAbsolutePath(), "META-INF", "MANIFEST.MF");
        File manifestFile = manifestPath.toFile();
        if (manifestFile.exists()) {
            manifest = new Manifest(new ByteArrayInputStream(Files.readAllBytes(manifestPath)));
        } else {
            manifest = new Manifest();
            manifestFile.getParentFile().mkdirs();
        }

        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0"); // jdk

        // 从原 Manifest 中获取版本号
        String version = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        if (version == null || version.isEmpty()) {
            version = attributes.getValue(Constants.BUNDLE_VERSION);
        }
        if (version == null || version.isEmpty()) {
            version = "1.0.0";
        }

        attributes.putValue(Constants.BUNDLE_SYMBOLICNAME, targetJar.getName().substring(0, targetJar.getName().length() - ".jar".length())); // osgi bundle
        attributes.putValue(Constants.BUNDLE_VERSION_ATTRIBUTE, version); // osgi bundle
        attributes.putValue(Constants.BUNDLE_ACTIVATOR, "qingzhou.app.driver.AppDriver"); // osgi bundle

        // 此处配置的导入须是 qingzhou-app-driver 直接依赖的，并且与 -Dqingzhou.osgi.packages.import.app 配置不冲突
        attributes.putValue(Constants.IMPORT_PACKAGE, "org.osgi.framework,org.osgi.service.cm," + // osgi
                "qingzhou.api,qingzhou.api.action," +
                "qingzhou.dto,qingzhou.dto.meta," +
                "qingzhou.common," +
                "qingzhou.registry," +
                "qingzhou.logger," +
                "qingzhou.json," +
                "qingzhou.crypto"
        );
        if (appDynamicPackages == null) {
            String pkgExtra = System.getProperty("qingzhou.osgi.packages.import.system");
            appDynamicPackages = pkgExtra != null && !pkgExtra.trim().isEmpty() ? pkgExtra.trim() : "";

            String dynamicPkgs = System.getProperty("qingzhou.osgi.packages.import.app");
            if (dynamicPkgs != null && !dynamicPkgs.trim().isEmpty()) {
                appDynamicPackages += ("," + dynamicPkgs.trim());
            }
        }
        if (!appDynamicPackages.isEmpty()) { // 无需动态导入时不写入空值，避免解析出空 Package 信息
            attributes.putValue(Constants.DYNAMICIMPORT_PACKAGE, appDynamicPackages);
        }

        try (OutputStream fos = Files.newOutputStream(manifestPath)) {
            manifest.write(fos);
        }
    }

    private void addDriverClasses(File appTmpDir, String libDir) throws Exception {
        File[] driverJarFiles = Paths.get(libDir, "runtime", "app-driver").toFile().listFiles();
        if (driverJarFiles == null) {
            throw new IllegalStateException("app-driver directory not found under: " + libDir);
        }
        for (File driverJarFile : driverJarFiles) {
            // overwrite=true 覆盖同名 class（防应用遮蔽驱动实现）；false 跳过已存在项（保留应用侧自定义文件）
            // driver 覆盖解压也不能碰 MANIFEST.MF：应用可能在 manifest 中声明 Qingzhou-Detection-Enabled 等自定义 headers
            FileUtil.unZipToDir(driverJarFile, appTmpDir, true, "META-INF/MANIFEST.MF");
        }
    }

    private void addAnnotationFile(File appTmpDir, File targetJar) throws Exception {
        Set<String> allClassNames;
        try (Stream<Path> paths = Files.walk(appTmpDir.toPath())) {
            String classSuffix = ".class";
            allClassNames = paths.filter(p -> p.toString().endsWith(classSuffix))
                    .map(path -> {
                        String fullPath = path.toFile().getAbsolutePath();
                        String classFile = fullPath.substring(
                                appTmpDir.getAbsolutePath().length() + File.separator.length(),
                                fullPath.length() - classSuffix.length());
                        return classFile.replace(File.separator, ".");
                    })
                    .collect(Collectors.toSet());
        }

        String jarNameDefaultCode = targetJar.getName();
        if (jarNameDefaultCode.endsWith(".jar")) {
            jarNameDefaultCode = jarNameDefaultCode.substring(0, jarNameDefaultCode.length() - ".jar".length());
        }

        qingzhou.dto.meta.App app = new qingzhou.dto.meta.App();
        for (String cls : allClassNames) {
            CtClass ctClass = classPool.get(cls);
            try {
                parseAnnotations(ctClass, app, jarNameDefaultCode);
            } finally {
                ctClass.detach();
            }
        }
        if (app.className == null || app.className.trim().isEmpty()) {
            throw new IllegalStateException("Missing @App:");
        }

        Path jsonPath = Paths.get(appTmpDir.getAbsolutePath(), "QZ-INF", "annotation.json");
        jsonPath.toFile().getParentFile().mkdirs();
        JsonImpl jsonImpl = new JsonImpl();
        jsonImpl.init();
        String json = jsonImpl.toJson(app);
        Files.write(jsonPath, json.getBytes(StandardCharsets.UTF_8));
    }

    private void parseAnnotations(CtClass ctClass, qingzhou.dto.meta.App metaApp, String defaultCode) throws Exception {
        App app = (App) ctClass.getAnnotation(App.class);
        if (app != null) {
            if (metaApp.className != null) {
                throw new IllegalStateException("Duplicate annotation @App:"
                        + metaApp.className
                        + System.lineSeparator()
                        + ctClass.getName());
            }
            metaApp.className = ctClass.getName();
            setObjAnnotation(metaApp, app);
            if (metaApp.code == null || metaApp.code.isEmpty()) {
                metaApp.code = defaultCode;
            }
        }

        Model model = (Model) ctClass.getAnnotation(Model.class);
        if (model != null) {
            parseModelAnnotations(model, ctClass, metaApp);
        }

        // 可重复注解在 class 文件里有两种形态：单次使用为裸注解，多次使用为编译器合成的容器注解
        // 按类型定向读取：getAnnotations() 会解析类上全部注解，应用侧 OSGi 等注解不在转换器 classpath 中
        Menu menu = (Menu) ctClass.getAnnotation(Menu.class);
        Menus menus = (Menus) ctClass.getAnnotation(Menus.class);
        I18n i18n = (I18n) ctClass.getAnnotation(I18n.class);
        I18ns i18ns = (I18ns) ctClass.getAnnotation(I18ns.class);
        if (menu != null) {
            addMenu(menu, metaApp);
        }
        if (menus != null) {
            for (Menu item : menus.value()) {
                addMenu(item, metaApp);
            }
        }
        if (i18n != null) {
            addI18n(i18n, metaApp);
        }
        if (i18ns != null) {
            for (I18n item : i18ns.value()) {
                addI18n(item, metaApp);
            }
        }
    }

    private void addMenu(Menu menu, qingzhou.dto.meta.App metaApp) throws Exception {
        qingzhou.dto.meta.Menu dtoMenu = new qingzhou.dto.meta.Menu();
        setObjAnnotation(dtoMenu, menu);
        metaApp.menus.add(dtoMenu);
    }

    private void addI18n(I18n i18n, qingzhou.dto.meta.App metaApp) throws Exception {
        qingzhou.dto.meta.I18n dtoI18n = new qingzhou.dto.meta.I18n();
        setObjAnnotation(dtoI18n, i18n);
        metaApp.i18ns.add(dtoI18n);
    }

    private void parseModelAnnotations(Model model, CtClass ctClass, qingzhou.dto.meta.App metaApp) throws Exception {
        qingzhou.dto.meta.Model dtoModel = new qingzhou.dto.meta.Model();
        dtoModel.className = ctClass.getName();
        setObjAnnotation(dtoModel, model);
        if (dtoModel.code == null || dtoModel.code.isEmpty()) {
            dtoModel.code = ctClass.getSimpleName();
        }
        if (!metaApp.models.add(dtoModel)) {
            throw new IllegalStateException("Duplicate annotation @Model:" + dtoModel.code);
        }

        // 默认 Action 以 DefaultAction 方法签名为唯一来源：模型实现首参能力接口即注入
        for (CtMethod ctMethod : classPool.get(DefaultAction.class.getName()).getMethods()) {
            ModelAction methodAnnotation = (ModelAction) ctMethod.getAnnotation(ModelAction.class);
            if (methodAnnotation == null) continue;
            CtClass[] parameterTypes = ctMethod.getParameterTypes();
            if (parameterTypes.length != 2 || !ctClass.subtypeOf(parameterTypes[0])) continue;

            qingzhou.dto.meta.ModelAction dtoModelAction = new qingzhou.dto.meta.ModelAction();
            dtoModelAction.isDefaultAction = true;
            dtoModelAction.methodName = ctMethod.getName();
            setObjAnnotation(dtoModelAction, methodAnnotation);
            if (dtoModelAction.code == null || dtoModelAction.code.isEmpty()) {
                dtoModelAction.code = ctMethod.getName();
            }
            dtoModel.actions.add(dtoModelAction);
        }

        // 类级别 @ModelAction 仅覆盖默认 action 的元数据，执行逻辑仍走 DefaultAction
        for (ModelAction a : classModelActions(ctClass)) {
            applyClassAction(a, dtoModel.actions);
        }

        for (CtMethod ctMethod : ctClass.getMethods()) {
            ModelAction modelAction = (ModelAction) ctMethod.getAnnotation(ModelAction.class);
            if (modelAction != null) {
                qingzhou.dto.meta.ModelAction dtoModelAction = new qingzhou.dto.meta.ModelAction();
                dtoModelAction.methodName = ctMethod.getName();
                setObjAnnotation(dtoModelAction, modelAction);
                if (dtoModelAction.code == null || dtoModelAction.code.isEmpty()) {
                    dtoModelAction.code = ctMethod.getName();
                }
                dtoModel.actions.remove(dtoModelAction); // 若已经添加了默认的 Action，允许应用自定义覆盖
                dtoModel.actions.add(dtoModelAction);
            }
        }

        for (CtField ctField : ctClass.getFields()) {
            ModelField modelField = (ModelField) ctField.getAnnotation(ModelField.class);
            if (modelField != null) {
                qingzhou.dto.meta.ModelField dtoModelField = new qingzhou.dto.meta.ModelField();
                dtoModelField.fieldName = ctField.getName();
                setObjAnnotation(dtoModelField, modelField);
                if (dtoModelField.code == null || dtoModelField.code.isEmpty()) {
                    dtoModelField.code = ctField.getName();
                }
                if (dtoModel.fields.contains(dtoModelField)) {
                    throw new IllegalStateException("Duplicate annotation @ModelField:" + dtoModelField.code);
                }
                dtoModel.fields.add(dtoModelField);
            }
        }
    }

    private void setObjAnnotation(Object object, Annotation annotation) throws Exception {
        setObjAnnotation(object, annotation, false);
    }

    // 仅覆盖注解中显式声明（非默认值）的字段，保留 DTO 已有值
    private void mergeObjAnnotation(Object object, Annotation annotation) throws Exception {
        setObjAnnotation(object, annotation, true);
    }

    // 方法名 = 注解属性名 = DTO 字段名；declaredOnly 时跳过仍为注解默认值的字段
    private void setObjAnnotation(Object object, Annotation annotation, boolean declaredOnly) throws Exception {
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            Object value = method.invoke(annotation);
            if (declaredOnly && java.util.Objects.deepEquals(value, method.getDefaultValue())) continue;
            object.getClass().getField(method.getName()).set(object, value);
        }
    }

    // 编译器对单个 @ModelAction 直接存储，多个打包成 @OverrideDefaultActions 容器
    private static java.util.List<ModelAction> classModelActions(CtClass ctClass) throws Exception {
        ModelAction single = (ModelAction) ctClass.getAnnotation(ModelAction.class);
        if (single != null) return java.util.Collections.singletonList(single);
        OverrideDefaultActions container = (OverrideDefaultActions) ctClass.getAnnotation(OverrideDefaultActions.class);
        return container == null ? java.util.Collections.emptyList() : java.util.Arrays.asList(container.value());
    }

    private void applyClassAction(ModelAction classAction, Set<qingzhou.dto.meta.ModelAction> defaultActions) throws Exception {
        for (qingzhou.dto.meta.ModelAction dto : defaultActions) {
            if (dto.isDefaultAction && classAction.code().equals(dto.code)) {
                mergeObjAnnotation(dto, classAction);
                return;
            }
        }
    }

}
