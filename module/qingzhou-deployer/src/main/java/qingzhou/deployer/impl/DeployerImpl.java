package qingzhou.deployer.impl;

import qingzhou.api.*;
import qingzhou.api.type.Add;
import qingzhou.api.type.Grouped;
import qingzhou.api.type.List;
import qingzhou.api.type.Validate;
import qingzhou.deployer.AppListener;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.Utils;
import qingzhou.logger.Logger;
import qingzhou.registry.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
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
    // 同 qingzhou.registry.impl.RegistryImpl.registryInfo 使用自然排序，以支持分页
    private final Map<String, AppImpl> apps = new TreeMap<>();

    private final ModuleContext moduleContext;
    private final Logger logger;
    private LoaderPolicy loaderPolicy;

    private final java.util.List<AppListener> appListeners = new ArrayList<>();

    DeployerImpl(ModuleContext moduleContext, Logger logger) {
        this.moduleContext = moduleContext;
        this.logger = logger;
    }

    @Override
    public void addAppListener(AppListener appListener) {
        appListeners.add(appListener);
    }

    @Override
    public void installApp(File appDir) throws Exception {
        if (!appDir.isDirectory()) throw new IllegalArgumentException("The app file must be a directory");

        AppImpl app = buildApp(appDir);

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
        String name = app.getAppInfo().getName();
        apps.put(name, app);
        appListeners.forEach(appListener -> appListener.onInstalled(name));

        logger.info("The app has been successfully installed: " + appDir.getName());
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
        app.getAppInfo().setState(DeployerConstants.app_Started);
    }

    @Override
    public void unInstallApp(String appName, boolean delete) throws Exception {
        AppImpl app = null;
        if (delete) {
            app = apps.remove(appName);
        } else {
            app = apps.get(appName);
        }
        if (app == null) return;

        app.getModelBaseMap().values().forEach(ModelBase::stop);

        QingzhouApp qingzhouApp = app.getQingzhouApp();
        if (qingzhouApp != null) {
            qingzhouApp.stop();
            app.getAppInfo().setState(DeployerConstants.app_Stopped);
        }

        try {
            app.getLoader().close();
        } catch (Exception ignored) {
        }

        appListeners.forEach(appListener -> appListener.onUninstalled(appName));
    }

    @Override
    public java.util.List<String> getAllApp() {
        return new ArrayList<>(apps.keySet());
    }

    @Override
    public qingzhou.deployer.App getApp(String appName) {
        return apps.get(appName);
    }

    private AppImpl buildApp(File appDir) throws Exception {
        AppImpl app = new AppImpl();

        java.util.List<File> scanAppLibFiles = new ArrayList<>();
        findLib(appDir, scanAppLibFiles);
        File[] additionalLib = loaderPolicy.getAdditionalLib();
        if (additionalLib != null) {
            scanAppLibFiles.addAll(Arrays.asList(additionalLib));
        }
        File[] appLibs = scanAppLibFiles.toArray(new File[0]);

        URLClassLoader loader = buildLoader(appLibs);
        app.setLoader(loader);

        QingzhouApp qingzhouApp = buildQingzhouApp(appLibs, loader);
        if (qingzhouApp instanceof QingzhouSystemApp) {
            QingzhouSystemApp qingzhouSystemApp = (QingzhouSystemApp) qingzhouApp;
            qingzhouSystemApp.setModuleContext(moduleContext);
        }
        app.setQingzhouApp(qingzhouApp);

        AppInfo appInfo = new AppInfo();
        appInfo.setName(appDir.getName());
        appInfo.setFilePath(appDir.getAbsolutePath());
        Map<ModelBase, ModelInfo> modelInfos = getModelInfos(appLibs, loader);
        modelInfos.values().forEach(appInfo::addModelInfo);
        app.setAppInfo(appInfo);

        AppContextImpl appContext = buildAppContext(app);
        appContext.setAppDir(appDir);
        app.setAppContext(appContext);

        modelInfos.forEach((key, value) -> app.getModelBaseMap().put(value.getCode(), key));

        // 构建 Action 执行器
        modelInfos.forEach((modelBase, modelInfo) -> {
            Map<String, ActionMethod> moelMap = app.getModelActionMap().computeIfAbsent(modelInfo.getCode(), model -> new HashMap<>());
            for (ModelActionInfo action : modelInfo.getModelActionInfos()) {
                ActionMethod actionMethod = ActionMethod.buildActionMethod(action.getMethod(), modelBase);
                moelMap.put(action.getCode(), actionMethod);
            }
        });

        // 追加默认的 Action 执行器
        Map<String, java.util.List<ModelActionInfo>> addedDefaultActions = addDefaultAction(modelInfos);// 追加系统预置的 action
        addedDefaultActions.forEach((modelName, addedModelActions) -> {
            ModelBase[] findModelBase = new ModelBase[1];
            modelInfos.entrySet().stream().filter(entry -> entry.getValue().getCode().equals(modelName)).findAny().ifPresent(entry -> findModelBase[0] = entry.getKey());
            DefaultAction defaultAction = new DefaultAction(app, findModelBase[0]);
            Map<String, ActionMethod> moelMap = app.getModelActionMap().computeIfAbsent(modelName, model -> new HashMap<>());
            for (ModelActionInfo action : addedModelActions) {
                ActionMethod actionMethod = ActionMethod.buildActionMethod(action.getMethod(), defaultAction);
                moelMap.put(action.getCode(), actionMethod);
            }
        });

        return app;
    }

    private Map<String, java.util.List<ModelActionInfo>> addDefaultAction(Map<ModelBase, ModelInfo> modelInfos) {
        Map<String, java.util.List<ModelActionInfo>> addActionToModels = detectActionsToAdd(modelInfos);
        mergeDefaultActions(modelInfos, addActionToModels);
        return addActionToModels;
    }

    private Map<String, java.util.List<ModelActionInfo>> detectActionsToAdd(Map<ModelBase, ModelInfo> forModels) {
        Map<String, java.util.List<ModelActionInfo>> addActionToModels = new HashMap<>();
        for (Map.Entry<ModelBase, ModelInfo> entry : forModels.entrySet()) {
            java.util.List<ModelActionInfo> added = new ArrayList<>();
            Set<ModelActionInfo> selfActions = Arrays.stream(entry.getValue().getModelActionInfos()).collect(Collectors.toSet());
            ModelBase modelBase = entry.getKey();
            Set<String> detectedActionNames = new HashSet<>();
            findSuperDefaultActions(modelBase.getClass(), detectedActionNames);
            detectedActionNames.forEach(addActionName -> {
                boolean exists = false;
                for (ModelActionInfo self : selfActions) {
                    if (addActionName.equals(self.getCode())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    for (ModelActionInfo actionInfo : DefaultAction.allDefaultActionCache) {
                        if (addActionName.equals(actionInfo.getCode())) {
                            added.add(actionInfo);
                            break;
                        }
                    }
                }
            });

            addActionToModels.put(entry.getValue().getCode(), added);
        }
        return addActionToModels;
    }

    private void mergeDefaultActions(Map<ModelBase, ModelInfo> modelInfos, Map<String, java.util.List<ModelActionInfo>> addActionToModels) {
        for (Map.Entry<String, java.util.List<ModelActionInfo>> addedModelActionInfos : addActionToModels.entrySet()) {
            ModelInfo addedToModel = null;
            for (ModelInfo value : modelInfos.values()) {
                if (value.getCode().equals(addedModelActionInfos.getKey())) {
                    addedToModel = value;
                    break;
                }
            }
            java.util.List<ModelActionInfo> modelActionInfoList = new ArrayList<>(Arrays.asList(Objects.requireNonNull(addedToModel).getModelActionInfos()));
            modelActionInfoList.addAll(addedModelActionInfos.getValue());
            addedToModel.setModelActionInfos(modelActionInfoList.toArray(new ModelActionInfo[0]));
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
            if (instance instanceof List) {
                modelInfo.setIdField(((List) instance).idField());
            }
            modelInfo.setModelFieldInfos(getModelFieldInfos(annotation, instance));
            java.util.List<ModelActionInfo> methodModelActionInfoMap = parseModelActionInfos(annotation);
            modelInfo.setModelActionInfos(methodModelActionInfoMap.toArray(new ModelActionInfo[0]));
            modelInfo.setGroupInfos(getGroupInfo(instance));
            modelInfo.setValidate(instance instanceof Validate);
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
        java.util.List<GroupInfo> groupInfoList = new ArrayList<>();
        if (instance instanceof Grouped) {
            Groups groups = ((Grouped) instance).groups();
            if (groups != null) {
                groups.groups().stream().map(GroupInfo::new).forEach(groupInfoList::add);
            }
        }
        return groupInfoList.toArray(new GroupInfo[0]);
    }

    private ModelFieldInfo[] getModelFieldInfos(AnnotationReader annotation, ModelBase instance) {
        java.util.List<ModelFieldInfo> modelFieldInfoList = new ArrayList<>();
        annotation.readModelField().forEach((field, modelField) -> {
            ModelFieldInfo modelFieldInfo = new ModelFieldInfo();
            modelFieldInfo.setCode(field.getName());
            modelFieldInfo.setName(modelField.name());
            modelFieldInfo.setInfo(modelField.info());
            modelFieldInfo.setGroup(modelField.group());
            modelFieldInfo.setType(modelField.type().name());
            modelFieldInfo.setOptions(modelField.options());
            modelFieldInfo.setRefModelClass(modelField.refModel());
            modelFieldInfo.setSeparator(modelField.separator());
            modelFieldInfo.setDefaultValue(getDefaultValue(field, instance));
            modelFieldInfo.setList(modelField.list());
            modelFieldInfo.setMonitor(modelField.monitor());
            modelFieldInfo.setNumeric(modelField.numeric());
            modelFieldInfo.setCreate(modelField.create());
            modelFieldInfo.setEdit(modelField.edit());
            modelFieldInfo.setRequired(modelField.required());
            modelFieldInfo.setMin(modelField.min());
            modelFieldInfo.setMax(modelField.max());
            modelFieldInfo.setLengthMin(modelField.lengthMin());
            modelFieldInfo.setLengthMax(modelField.lengthMax());
            modelFieldInfo.setPattern(modelField.pattern());
            modelFieldInfo.setHost(modelField.host());
            modelFieldInfo.setPort(modelField.port());
            modelFieldInfo.setNoChar(modelField.noChar());
            modelFieldInfo.setNoString(modelField.noString());
            modelFieldInfo.setShow(modelField.show());
            modelFieldInfo.setReadOnly(modelField.readonly());
            modelFieldInfo.setEmail(modelField.email());
            modelFieldInfo.setFile(modelField.file());
            modelFieldInfo.setLink(modelField.link());
            modelFieldInfo.setColor(modelField.color());
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
            throw new IllegalStateException("An app must have and can only have one implementation class for " + QingzhouApp.class.getName());
        }
    }

    private AppContextImpl buildAppContext(AppImpl app) {
        //        appContext.addActionFilter(new UniqueFilter(appContext));
        return new AppContextImpl(moduleContext, app);
    }

    private void findLib(File libFile, java.util.List<File> libs) throws IOException {
        libs.add(libFile);

        if (libFile.isDirectory()) {
            File[] listFiles = libFile.listFiles();
            if (listFiles != null) {
                for (File f : listFiles) {
                    findLib(f, libs);
                }
            }
            return;
        }

        if (!libFile.getName().endsWith(".jar")) return;
        libs.addAll(parseManifestLib(libFile));
    }

    private java.util.List<File> parseManifestLib(File appFile) throws IOException {
        java.util.List<File> libs = new ArrayList<>();
        try (JarFile jarFile = new JarFile(appFile)) {
            Manifest manifest = jarFile.getManifest();
            Attributes mainAttributes = manifest.getMainAttributes();
            String classPathValue = mainAttributes.getValue(Attributes.Name.CLASS_PATH);
            if (classPathValue == null || classPathValue.isEmpty()) {
                return libs;
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
                if (file.exists()) {
                    libs.add(file);
                }
            }
        }
        return libs;
    }

    private URLClassLoader buildLoader(File[] appLibs) {
        URL[] urls = Arrays.stream(appLibs).map(file -> {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).toArray(URL[]::new);

        return new URLClassLoader(urls, loaderPolicy.getClassLoader());
    }

    static java.util.List<ModelActionInfo> parseModelActionInfos(AnnotationReader annotation) {
        java.util.List<ModelActionInfo> modelActionInfos = new ArrayList<>();
        annotation.readModelAction().forEach((method, modelAction) -> {
            if (modelAction.disable()) return;
            ModelActionInfo modelActionInfo = new ModelActionInfo();
            modelActionInfo.setMethod(method);
            modelActionInfo.setCode(modelAction.code());
            modelActionInfo.setName(modelAction.name());
            modelActionInfo.setInfo(modelAction.info());
            modelActionInfo.setIcon(modelAction.icon());
            modelActionInfo.setOrder(modelAction.order());
            modelActionInfo.setHead(modelAction.head());
            modelActionInfo.setList(modelAction.list());
            modelActionInfo.setBatch(modelAction.batch());
            modelActionInfo.setShow(modelAction.show());
            modelActionInfo.setPage(modelAction.page());
            modelActionInfo.setFields(modelAction.fields());
            modelActionInfos.add(modelActionInfo);
        });
        return modelActionInfos;
    }

    static void findSuperDefaultActions(Class<?> checkClass, Set<String> defaultActions) {
        Set<String> foundActions = findDefaultActions(checkClass);
        if (foundActions != null) {
            defaultActions.addAll(foundActions);
        }

        Class<?> superClass = checkClass.getSuperclass();
        if (superClass != null) {
            findSuperDefaultActions(superClass, defaultActions);
        }

        for (Class<?> c : checkClass.getInterfaces()) {
            findSuperDefaultActions(c, defaultActions);
        }
    }

    private static Set<String> findDefaultActions(Class<?> checkClass) {
        if (!checkClass.isInterface()) return null;
        if (checkClass.getPackage() != (Add.class.getPackage())) return null;

        Set<String> defaultActions = new HashSet<>();
        for (Field field : checkClass.getDeclaredFields()) {
            if (field.getName().startsWith("ACTION_")) {
                try {
                    defaultActions.add((String) field.get(null));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return defaultActions;
    }

    void setLoaderPolicy(LoaderPolicy loaderPolicy) {
        this.loaderPolicy = loaderPolicy;
    }

    interface LoaderPolicy {
        ClassLoader getClassLoader();

        File[] getAdditionalLib();
    }
}
