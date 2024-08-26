package qingzhou.deployer.impl;

import qingzhou.api.*;
import qingzhou.api.type.*;
import qingzhou.deployer.App;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.logger.Logger;
import qingzhou.registry.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

class DeployerImpl implements Deployer {

    private final Map<Method, ModelActionInfo> allDefaultActionCache;

    private final Map<String, App> apps = new HashMap<>();
    private final ModuleContext moduleContext;
    private final Logger logger;


    DeployerImpl(ModuleContext moduleContext, Logger logger) {
        this.moduleContext = moduleContext;
        this.logger = logger;
        this.allDefaultActionCache = parseModelActionInfos(new AnnotationReader(DefaultAction.class));
    }

    @Override
    public void installApp(File appDir) throws Exception {
        String appName = appDir.getName();
        if (apps.containsKey(appName)) {
            throw new IllegalArgumentException("The app already exists: " + appName);
        }
        boolean isSystemApp = "master".equals(appName) || "instance".equals(appName);
        AppImpl app = buildApp(appName, appDir, isSystemApp);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // 启动应用
        try {
            Thread.currentThread().setContextClassLoader(app.getLoader());
            startApp(app);
            // 初始化各模块
            startModel(app);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }

        // 注册完成
        apps.put(appName, app);

        logger.info("The app has been successfully installed: " + appName);
    }

    private void startModel(AppImpl app) throws Exception {
        Field appContextField = Arrays.stream(ModelBase.class.getDeclaredFields())
                .filter(field -> field.getType() == AppContext.class)
                .findFirst().orElseThrow((Supplier<Exception>) () -> new IllegalStateException("not found: " + app.getAppInfo().getName()));

        app.getModelBaseMap().values().forEach(modelBase -> {
            try {
                appContextField.setAccessible(true);
                appContextField.set(modelBase, app.getAppContext());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            modelBase.start();
        });
    }

    private void startApp(AppImpl app) throws Exception {
        app.getQingzhouApp().start(app.getAppContext());
    }

    @Override
    public void unInstallApp(String appName) throws Exception {
        AppImpl app = (AppImpl) apps.remove(appName);
        if (app == null) return;

        QingzhouApp qingzhouApp = app.getQingzhouApp();
        if (qingzhouApp != null) {
            qingzhouApp.stop();
        }

        try {
            app.getLoader().close();
        } catch (Exception ignored) {
        }
    }

    @Override
    public Collection<String> getAllApp() {
        return apps.keySet();
    }

    @Override
    public App getApp(String name) {
        return apps.get(name);
    }

    private AppImpl buildApp(String appName, File appDir, boolean isSystemApp) throws Exception {
        AppImpl app = new AppImpl();

        List<File> scanAppFilesCache = new ArrayList<>();
        File[] appLibs = buildLib(appDir, isSystemApp, scanAppFilesCache);
        if (appLibs.length == 0) {
            throw new IllegalArgumentException("The app[" + appName + "] jar file was not found");
        }
        URLClassLoader loader = buildLoader(appLibs, isSystemApp);
        app.setLoader(loader);

        File[] scanAppFiles = scanAppFilesCache.toArray(new File[0]);
        QingzhouApp qingzhouApp = buildQingzhouApp(scanAppFiles, loader);
        if (qingzhouApp instanceof QingzhouSystemApp) {
            QingzhouSystemApp qingzhouSystemApp = (QingzhouSystemApp) qingzhouApp;
            qingzhouSystemApp.setModuleContext(moduleContext);
        }
        app.setQingzhouApp(qingzhouApp);

        AppInfo appInfo = new AppInfo();
        appInfo.setName(appName);
        Map<ModelBase, ModelInfo> modelInfos = getModelInfos(scanAppFiles, loader);
        appInfo.setModelInfos(modelInfos.values());
        app.setAppInfo(appInfo);

        AppContextImpl appContext = buildAppContext(app);
        appContext.setAppDir(appDir);
        app.setAppContext(appContext);

        modelInfos.forEach((key, value) -> app.getModelBaseMap().put(value.getCode(), key));

        // 构建 Action 执行器
        modelInfos.forEach((modelBase, modelInfo) -> {
            Map<String, ActionMethod> moelMap = app.getModelActionMap().computeIfAbsent(modelInfo.getCode(), model -> new HashMap<>());
            for (ModelActionInfo action : modelInfo.getModelActionInfos()) {
                ActionMethod actionMethod = ActionMethod.buildActionMethod(action.getCode(), modelBase);
                moelMap.put(action.getCode(), actionMethod);
            }
        });

        // 构建 Action 执行器
        Map<String, Collection<ModelActionInfo>> addedDefaultActions = addDefaultAction(modelInfos);// 追加系统预置的 action
        addedDefaultActions.forEach((modelName, addedModelActions) -> {
            ModelBase[] findModelBase = new ModelBase[1];
            modelInfos.entrySet().stream().filter(entry -> entry.getValue().getCode().equals(modelName)).findAny().ifPresent(entry -> findModelBase[0] = entry.getKey());
            DefaultAction defaultAction = new DefaultAction(app, findModelBase[0]);
            Map<String, ActionMethod> moelMap = app.getModelActionMap().computeIfAbsent(modelName, model -> new HashMap<>());
            for (ModelActionInfo action : addedModelActions) {
                ActionMethod actionMethod = ActionMethod.buildActionMethod(action.getCode(), defaultAction);
                moelMap.put(action.getCode(), actionMethod);
            }
        });

        return app;
    }

    private Map<String, Collection<ModelActionInfo>> addDefaultAction(Map<ModelBase, ModelInfo> modelInfos) {
        Map<String, Collection<ModelActionInfo>> addActionToModels = detectActionsToAdd(modelInfos);
        mergeDefaultActions(modelInfos, addActionToModels);
        return addActionToModels;
    }

    private Map<String, Collection<ModelActionInfo>> detectActionsToAdd(Map<ModelBase, ModelInfo> forModels) {
        Map<String, Collection<ModelActionInfo>> addActionToModels = new HashMap<>();
        for (Map.Entry<ModelBase, ModelInfo> entry : forModels.entrySet()) {
            List<ModelActionInfo> added = new ArrayList<>();
            Set<ModelActionInfo> selfActions = Arrays.stream(entry.getValue().getModelActionInfos()).collect(Collectors.toSet());
            ModelBase modelBase = entry.getKey();
            Set<String> detectedActions = new HashSet<>();
            findDefaultAction(modelBase.getClass(), detectedActions);
            detectedActions.forEach(addAction -> {
                boolean exists = false;
                for (ModelActionInfo self : selfActions) {
                    if (addAction.equals(self.getCode())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    for (Map.Entry<Method, ModelActionInfo> ma : allDefaultActionCache.entrySet()) {
                        if (addAction.equals(ma.getValue().getCode())) {
                            added.add(ma.getValue());
                            break;
                        }
                    }
                }
            });

            addActionToModels.put(entry.getValue().getCode(), added);
        }
        return addActionToModels;
    }

    private void mergeDefaultActions(Map<ModelBase, ModelInfo> modelInfos, Map<String, Collection<ModelActionInfo>> addActionToModels) {
        for (Map.Entry<String, Collection<ModelActionInfo>> addedModelActionInfos : addActionToModels.entrySet()) {
            ModelInfo addedToModel = null;
            for (ModelInfo value : modelInfos.values()) {
                if (value.getCode().equals(addedModelActionInfos.getKey())) {
                    addedToModel = value;
                    break;
                }
            }
            List<ModelActionInfo> modelActionInfoList = new ArrayList<>(Arrays.asList(Objects.requireNonNull(addedToModel).getModelActionInfos()));
            modelActionInfoList.addAll(addedModelActionInfos.getValue());
            addedToModel.setModelActionInfos(modelActionInfoList.toArray(new ModelActionInfo[0]));
        }
    }

    private void findDefaultAction(Class<?> checkClass, Set<String> defaultActions) {
        if (checkClass == Addable.class) {
            defaultActions.add("create");
            defaultActions.add("add");
        } else if (checkClass == Deletable.class) {
            defaultActions.add("delete");
        } else if (checkClass == Downloadable.class) {
            defaultActions.add("files");
            defaultActions.add("download");
        } else if (checkClass == Listable.class) {
            defaultActions.add("list");
        } else if (checkClass == Monitorable.class) {
            defaultActions.add("monitor");
        } else if (checkClass == Showable.class) {
            defaultActions.add("show");
        } else if (checkClass == Updatable.class) {
            defaultActions.add("edit");
            defaultActions.add("update");
        }

        for (Class<?> c : checkClass.getInterfaces()) {
            findDefaultAction(c, defaultActions);
        }
    }

    private Map<ModelBase, ModelInfo> getModelInfos(File[] appLibs, URLClassLoader loader) throws Exception {
        Collection<String> modelClassName = Utils.detectAnnotatedClass(appLibs, Model.class, null, loader);

        Map<ModelBase, ModelInfo> modelInfos = new HashMap<>();

        Set<String> allCodes = new HashSet<>();
        for (String s : modelClassName) {
            Class<?> aClass = loader.loadClass(s);
            Model model = aClass.getDeclaredAnnotation(Model.class);

            if (!allCodes.add(model.code())) {
                throw new IllegalStateException("Duplicate model name: " + model.code());
            }

            ModelInfo modelInfo = new ModelInfo();
            modelInfo.setCode(model.code());
            modelInfo.setName(model.name());
            modelInfo.setInfo(model.info());
            modelInfo.setIcon(model.icon());
            modelInfo.setMenu(model.menu());
            modelInfo.setOrder(model.order());
            modelInfo.setEntrance(model.entrance());
            modelInfo.setHidden(model.hidden());

            AnnotationReader annotation = new AnnotationReader(aClass);
            ModelBase instance;
            if (!ModelBase.class.isAssignableFrom(aClass)) {
                throw new IllegalArgumentException("The class annotated by the @Model ( " + aClass.getName() + " ) needs to 'extends ModelBase'.");
            }
            try {
                instance = (ModelBase) aClass.newInstance();
            } catch (InstantiationException e) {
                throw new IllegalArgumentException("The class annotated by the @Model needs to have a public parameter-free constructor.", e);
            }
            if (instance instanceof Listable) {
                modelInfo.setIdFieldName(((Listable) instance).idFieldName());
            }
            modelInfo.setModelFieldInfos(getModelFieldInfos(annotation, instance));
            modelInfo.setModelActionInfos(parseModelActionInfos(annotation).values().toArray(new ModelActionInfo[0]));
            if (instance instanceof Updatable) {
                modelInfo.setGroupInfos(getGroupInfo(instance));
            }
            modelInfos.put(instance, modelInfo);
        }

        // 转换 refModel 属性
        modelInfos.forEach((modelBase, modelInfo) -> {
            for (ModelFieldInfo modelFieldInfo : modelInfo.getModelFieldInfos()) {
                Map.Entry<ModelBase, ModelInfo> modelInfoEntry;
                Class<?> refModelClass = modelFieldInfo.getRefModelClass();
                if (refModelClass == ModelBase.class
                        || !ModelBase.class.isAssignableFrom(refModelClass)) {
                    continue;
                }
                try {
                    modelInfoEntry = modelInfos.entrySet().stream().filter(entry -> refModelClass == entry.getKey().getClass()).findAny().orElseThrow((Supplier<Throwable>) () -> new IllegalArgumentException("Ref-Model-Class " + refModelClass + " not fouond"));
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                modelFieldInfo.setRefModel(modelInfoEntry.getValue().getCode());
            }
        });

        return modelInfos;
    }

    private GroupInfo[] getGroupInfo(ModelBase instance) {
        List<GroupInfo> groupInfoList = new ArrayList<>();
        Groups groups = ((Updatable) instance).groups();
        if (groups != null) {
            groups.groups().forEach(group -> {
                GroupInfo groupInfo = new GroupInfo();
                groupInfo.setName(group.name());
                groupInfo.setI18n(group.i18n());
                groupInfoList.add(groupInfo);
            });
        }
        return groupInfoList.toArray(new GroupInfo[0]);
    }

    private Map<Method, ModelActionInfo> parseModelActionInfos(AnnotationReader annotation) {
        Map<Method, ModelActionInfo> modelActionInfos = new HashMap<>();
        annotation.readModelAction().forEach((method, modelAction) -> {
            ModelActionInfo modelActionInfo = new ModelActionInfo();
            modelActionInfo.setCode(method.getName());
            modelActionInfo.setName(modelAction.name());
            modelActionInfo.setInfo(modelAction.info());
            modelActionInfo.setIcon(modelAction.icon());
            modelActionInfo.setOrder(modelAction.order());
            modelActionInfo.setShow(modelAction.show());
            modelActionInfo.setBatch(modelAction.batch());
            modelActionInfo.setDisable(modelAction.disable());
            modelActionInfo.setAjax(modelAction.ajax());
            modelActionInfos.put(method, modelActionInfo);
        });
        return modelActionInfos;
    }

    private ModelFieldInfo[] getModelFieldInfos(AnnotationReader annotation, ModelBase instance) {
        List<ModelFieldInfo> modelFieldInfoList = new ArrayList<>();
        annotation.readModelField().forEach((field, modelField) -> {
            ModelFieldInfo modelFieldInfo = new ModelFieldInfo();
            modelFieldInfo.setCode(field.getName());
            modelFieldInfo.setName(modelField.name());
            modelFieldInfo.setInfo(modelField.info());
            modelFieldInfo.setGroup(modelField.group());
            modelFieldInfo.setType(modelField.type().name());
            modelFieldInfo.setOptions(modelField.options());
            modelFieldInfo.setRefModelClass(modelField.refModel());
            modelFieldInfo.setDefaultValue(getDefaultValue(field, instance));
            modelFieldInfo.setList(modelField.list());
            modelFieldInfo.setMonitor(modelField.monitor());
            modelFieldInfo.setNumeric(modelField.numeric());
            modelFieldInfo.setCreateable(modelField.createable());
            modelFieldInfo.setEditable(modelField.editable());
            modelFieldInfo.setRequired(modelField.required());
            modelFieldInfo.setMin(modelField.min());
            modelFieldInfo.setMax(modelField.max());
            modelFieldInfo.setLengthMin(modelField.lengthMin());
            modelFieldInfo.setLengthMax(modelField.lengthMax());
            modelFieldInfo.setPattern(modelField.pattern());
            modelFieldInfo.setPort(modelField.port());
            modelFieldInfo.setUnsupportedCharacters(modelField.unsupportedCharacters());
            modelFieldInfo.setUnsupportedStrings(modelField.unsupportedStrings());
            modelFieldInfo.setShow(modelField.show());
            modelFieldInfo.setEmail(modelField.email());
            modelFieldInfoList.add(modelFieldInfo);
        });
        return modelFieldInfoList.toArray(new ModelFieldInfo[0]);
    }

    private String getDefaultValue(Field field, ModelBase modelBase) {
        boolean accessible = field.isAccessible();
        try {
            if (!accessible) {
                field.setAccessible(true);
            }

            String fieldValue = "";
            Object fieldValObj = field.get(modelBase);
            if (fieldValObj != null) {
                fieldValue = String.valueOf(fieldValObj);
            }
            return fieldValue;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            if (!accessible) {
                field.setAccessible(false);
            }
        }
    }

    private QingzhouApp buildQingzhouApp(File[] appLibs, URLClassLoader loader) throws Exception {
        Collection<String> annotatedClass = Utils.detectAnnotatedClass(appLibs, qingzhou.api.App.class, null, loader);
        if (annotatedClass.size() == 1) {
            Class<?> cls = loader.loadClass(annotatedClass.iterator().next());
            return (QingzhouApp) cls.newInstance();
        } else {
            throw new IllegalStateException("An app must have and can only have one implementation class for qingzhou.api.QingzhouApp");
        }
    }

    private AppContextImpl buildAppContext(AppImpl app) {
        //        appContext.addActionFilter(new UniqueFilter(appContext));
        return new AppContextImpl(moduleContext, app);
    }

    private File[] buildLib(File appDir, boolean isSystemApp, List<File> scanAppFilesCache) throws IOException {
        File[] appFiles = appDir.listFiles();
        if (appFiles == null || appFiles.length == 0) {
            throw new IllegalArgumentException("app lib not found: " + appDir.getName());
        }
        List<File> libs = new ArrayList<>();
        for (File appFile : appFiles) {
            if (appFile.isDirectory()) {
                continue;
            }

            if (!appFile.getName().endsWith(".jar")) {
                continue;
            }
            libs.add(appFile);
            appendAppClassPath(appFile, libs);

            scanAppFilesCache.add(appFile);
        }

        forYunJian(appDir, libs);// todo 云鉴的定制需求？

        if (!isSystemApp) {
            File[] commonFiles = FileUtil.newFile(moduleContext.getLibDir(), "module", "qingzhou-deployer", "common").listFiles();
            if (commonFiles != null && commonFiles.length > 0) {
                List<File> commonModels = Arrays.asList(commonFiles);
                libs.addAll(commonModels);

                scanAppFilesCache.addAll(commonModels);
            }
        }

        return libs.toArray(new File[0]);
    }

    private void forYunJian(File appDir, List<File> libs) {
        File config = FileUtil.newFile(appDir, "config");
        if (config.exists()) {
            libs.add(config);
        }

        File lib = FileUtil.newFile(appDir, "lib");
        if (lib.isDirectory()) {
            File[] files = lib.listFiles();
            if (files != null) {
                Arrays.sort(files);// 排序A-Za-z
                libs.addAll(Arrays.asList(files));
            }
        }
    }

    private void appendAppClassPath(File appFile, List<File> libs) throws IOException {
        try (JarFile jarFile = new JarFile(appFile)) {
            Manifest manifest = jarFile.getManifest();
            Attributes mainAttributes = manifest.getMainAttributes();
            String classPathValue = mainAttributes.getValue(Attributes.Name.CLASS_PATH);
            if (classPathValue == null || classPathValue.isEmpty()) {
                return;
            }

            String[] classPathEntries = classPathValue.split(" ");
            File parentFile = appFile.getParentFile();
            for (String entry : classPathEntries) {
                File file = new File(entry);
                if (!file.isAbsolute()) {
                    if (entry.startsWith("./")) {
                        file = new File(parentFile, entry.substring(2));
                    } else {
                        file = new File(parentFile, entry);
                    }
                }
                libs.add(file);
            }
        }
    }

    private URLClassLoader buildLoader(File[] appLibs, boolean isSystemApp) {
        URL[] urls = Arrays.stream(appLibs).map(file -> {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).toArray(URL[]::new);
        return new URLClassLoader(urls,
                isSystemApp
                        ? QingzhouSystemApp.class.getClassLoader()
                        : QingzhouApp.class.getClassLoader());
    }
}
