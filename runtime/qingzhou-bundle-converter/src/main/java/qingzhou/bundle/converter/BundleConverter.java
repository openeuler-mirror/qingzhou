package qingzhou.bundle.converter;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javassist.*;
import org.osgi.framework.Constants;
import qingzhou.api.*;
import qingzhou.app.driver.DefaultAction;
import qingzhou.json.impl.JsonImpl;

public class BundleConverter {
    private ClassPool classPool;
    private final Map<String, qingzhou.dto.meta.annotation.ModelAction> defaultActionMetaCache = new HashMap<>();

    private File targetJar;
    private String libDir;

    private File qzAppTmp;

    public void build(File sourceJar, File targetJar, String libDir) throws Exception {
        this.targetJar = targetJar;
        this.libDir = libDir;

        // 待处理的原应用的 jar
        qzAppTmp = new File(targetJar.getParentFile(), targetJar.getName() + UUID.randomUUID());
        qzAppTmp.mkdirs();
        unZipToDir(sourceJar, qzAppTmp);

        // 生成注解文件
        if (classPool == null) {
            ClassPool.doPruning = true;
            classPool = ClassPool.getDefault();
            classPool.appendClassPath(new LoaderClassPath(this.getClass().getClassLoader()));
        }
        ClassPath appendedClassPath = classPool.appendClassPath(qzAppTmp.getAbsolutePath());
        addAnnotationFile();
        classPool.removeClassPath(appendedClassPath);

        // 放入 OSGI 驱动类
        addDriverClass();

        // 添加 MANIFEST.MF 中 OSGI 声明
        addManifest();

        // 构建为 bundle jar
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(targetJar.toPath()))) {
            for (File file : qzAppTmp.listFiles()) {
                zipFiles(zos, file, file.getName());
            }
        }
    }

    private void addManifest() throws Exception {
        Manifest manifest;
        Path manifestPath = Paths.get(qzAppTmp.getAbsolutePath(), "META-INF", "MANIFEST.MF");
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
        attributes.putValue(Constants.IMPORT_PACKAGE, "org.osgi.framework,org.osgi.service.cm," + // osgi
                "qingzhou.registry," +
                "qingzhou.api,qingzhou.api.type," +
                "qingzhou.dto,qingzhou.dto.meta,qingzhou.dto.meta.annotation");
        attributes.putValue(Constants.DYNAMICIMPORT_PACKAGE,
                "qingzhou.logger," +
                        "qingzhou.json," +
                        "qingzhou.xml," +
                        "qingzhou.jdbc," +
                        "qingzhou.crypto," +
                        "qingzhou.http.server," +
                        "qingzhou.http.client," +
                        "qingzhou.qr," +
                        "com.sun.management");

        try (OutputStream fos = Files.newOutputStream(manifestPath)) {
            manifest.write(fos);
        }
    }

    private void addDriverClass() throws Exception {
        File[] driverJarFiles = Paths.get(libDir, "runtime", "app-driver").toFile().listFiles();
        for (File driverJarFile : driverJarFiles) {
            unZipToDir(driverJarFile, qzAppTmp);
        }
    }

    private void addAnnotationFile() throws Exception {
        Set<String> allClassNames;
        try (Stream<Path> paths = Files.walk(qzAppTmp.toPath())) {
            String classSuffix = ".class";
            allClassNames = paths.filter(p -> p.toString().endsWith(classSuffix))
                    .map(path -> {
                        String fullPath = path.toFile().getAbsolutePath();
                        String classFile = fullPath.substring(
                                qzAppTmp.getAbsolutePath().length() + File.separator.length(),
                                fullPath.length() - classSuffix.length());
                        return classFile.replace(File.separator, ".");
                    })
                    .collect(Collectors.toSet());
        }

        qingzhou.dto.meta.annotation.App app = new qingzhou.dto.meta.annotation.App();
        for (String cls : allClassNames) {
            CtClass ctClass = classPool.get(cls);
            try {
                parseAnnotations(ctClass, app);
            } finally {
                ctClass.detach();
            }
        }
        if (app.className == null || app.className.trim().isEmpty()) {
            throw new IllegalStateException("Missing @App:");
        }

        Path jsonPath = Paths.get(qzAppTmp.getAbsolutePath(), "QZ-INF", "annotation.json");
        jsonPath.toFile().getParentFile().mkdirs();
        JsonImpl jsonImpl = new JsonImpl();
        jsonImpl.init();
        String json = jsonImpl.toJson(app);
        Files.write(jsonPath, json.getBytes(StandardCharsets.UTF_8));
    }

    private void parseAnnotations(CtClass ctClass, qingzhou.dto.meta.annotation.App metaApp) throws Exception {
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
                String name = targetJar.getName();
                if (name.endsWith(".jar")) {
                    name = name.substring(0, name.length() - 4);
                }
                metaApp.code = name;
            }
        }

        Model model = (Model) ctClass.getAnnotation(Model.class);
        if (model != null) {
            parseModelAnnotations(model, ctClass, metaApp);
        }

        Menus menus = (Menus) ctClass.getAnnotation(Menus.class);
        if (menus != null) {
            for (Menu menu : menus.value()) {
                qingzhou.dto.meta.annotation.Menu dtoMenu = new qingzhou.dto.meta.annotation.Menu();
                setObjAnnotation(dtoMenu, menu);
                metaApp.menus.add(dtoMenu);
            }
        }
    }

    private void parseModelAnnotations(Model model, CtClass ctClass, qingzhou.dto.meta.annotation.App metaApp) throws Exception {
        qingzhou.dto.meta.annotation.Model dtoModel = new qingzhou.dto.meta.annotation.Model();
        dtoModel.className = ctClass.getName();
        setObjAnnotation(dtoModel, model);
        if (dtoModel.code == null || dtoModel.code.isEmpty()) {
            dtoModel.code = ctClass.getSimpleName();
        }
        if (!metaApp.models.add(dtoModel)) {
            throw new IllegalStateException("Duplicate annotation @Model:" + dtoModel.code);
        }

        // 添加默认的 Action
        for (CtClass ctInterface : ctClass.getInterfaces()) {
            if (ctInterface.subtypeOf(classPool.get(QingzhouModel.class.getName()))) {
                for (CtField ctField : ctInterface.getFields()) {
                    String fieldName = ctField.getName();
                    if (fieldName.startsWith("ACTION_CODE_")) {
                        String fieldVal = (String) ctField.getConstantValue();
                        qingzhou.dto.meta.annotation.ModelAction modelAction = parseDefaultActionMeta(fieldVal);
                        if (modelAction != null) {
                            dtoModel.actions.add(modelAction);
                        }
                    }
                }
            }
        }

        for (CtMethod ctMethod : ctClass.getMethods()) {
            ModelAction modelAction = (ModelAction) ctMethod.getAnnotation(ModelAction.class);
            if (modelAction != null) {
                qingzhou.dto.meta.annotation.ModelAction dtoModelAction = new qingzhou.dto.meta.annotation.ModelAction();
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
                qingzhou.dto.meta.annotation.ModelField dtoModelField = new qingzhou.dto.meta.annotation.ModelField();
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

    private qingzhou.dto.meta.annotation.ModelAction parseDefaultActionMeta(String actionName) {
        return defaultActionMetaCache.computeIfAbsent(actionName, s -> {
            CtClass ctClassForDefaultAction = null;
            try {
                ctClassForDefaultAction = classPool.get(DefaultAction.class.getName());
                for (CtMethod ctMethod : ctClassForDefaultAction.getMethods()) {
                    if (ctMethod.getName().equals(actionName)) {
                        ModelAction methodAnnotation = (ModelAction) ctMethod.getAnnotation(ModelAction.class);
                        if (methodAnnotation != null) {
                            qingzhou.dto.meta.annotation.ModelAction dtoModelAction = new qingzhou.dto.meta.annotation.ModelAction();
                            dtoModelAction.isDefaultAction = true;
                            dtoModelAction.methodName = actionName;
                            setObjAnnotation(dtoModelAction, methodAnnotation);
                            if (dtoModelAction.code == null || dtoModelAction.code.isEmpty()) {
                                dtoModelAction.code = actionName;
                            }
                            return dtoModelAction;
                        }
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            } finally {
                if (ctClassForDefaultAction != null) {
                    ctClassForDefaultAction.detach();
                }
            }
            return null;
        });
    }

    private void setObjAnnotation(Object object, Annotation annotation) throws Exception {
        Method[] methods = annotation.annotationType().getDeclaredMethods();
        for (Method method : methods) {
            String fieldName = method.getName(); // 方法名 = 注解属性名
            Object fieldValue;
            try {
                fieldValue = method.invoke(annotation); // 调用方法获取属性值（无参，传null）
            } catch (Exception e) {
                // 这里会发生什么错误？
                // 答案：当注解属性值为默认值时，method.invoke 会抛出异常，因为默认值不是通过方法调用获取的
                // 如何获取默认值？
                // 答案：通过 method.getDefaultValue() 获取
                fieldValue = method.getDefaultValue();
            }
            object.getClass().getField(fieldName)
                    .set(object, fieldValue);
        }
    }

    private void unZipToDir(File srcFile, File unZipDir) throws IOException {
        try (ZipFile zip = new ZipFile(srcFile, ZipFile.OPEN_READ)) {
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                File targetFile = new File(unZipDir, entry.getName());
                if (entry.isDirectory()) {
                    targetFile.mkdirs();
                } else {
                    targetFile.getParentFile().mkdirs();
                    try (OutputStream out = Files.newOutputStream(targetFile.toPath())) {
                        copyStream(zip.getInputStream(entry), out);
                    }
                }
            }
        }
    }

    private void zipFiles(ZipOutputStream zos, File srcFile, String toZipName) throws IOException {
        if (srcFile.isDirectory()) {
            zos.putNextEntry(new ZipEntry(toZipName + "/"));
            File[] listFiles = srcFile.listFiles();
            if (listFiles != null) {
                for (File sub : listFiles) {
                    zipFiles(zos, sub, toZipName + "/" + sub.getName());
                }
            }
        } else {
            ZipEntry zipEntry = new ZipEntry(toZipName);
            zos.putNextEntry(zipEntry);
            try (InputStream in = Files.newInputStream(srcFile.toPath())) {
                copyStream(in, zos);
            }
        }
    }

    private void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024 * 4];
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        output.flush();
    }
}
