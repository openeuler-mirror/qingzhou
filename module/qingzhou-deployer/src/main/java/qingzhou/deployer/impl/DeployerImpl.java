package qingzhou.deployer.impl;

import qingzhou.api.*;
import qingzhou.api.type.Showable;
import qingzhou.deployer.App;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.Utils;
import qingzhou.registry.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class DeployerImpl implements Deployer {

    private final Map<Method, ModelActionInfo> presetMethodActionInfos;

    private final Map<String, App> apps = new HashMap<>();
    private final ModuleContext moduleContext;


    DeployerImpl(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
        this.presetMethodActionInfos = parseModelActionInfos(new AnnotationReader(PresetAction.class));
    }

    @Override
    public void installApp(File appDir) throws Exception {
        String appName = appDir.getName();
        try {
            if (apps.containsKey(appName)) {
                throw new IllegalArgumentException("The app already exists: " + appName);
            }
            boolean isSystemApp = DeployerConstants.MASTER_APP_NAME.equals(appName) || DeployerConstants.INSTANCE_APP_NAME.equals(appName);
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
        } catch (Exception e) {
            //new Exception("failed to install app " + appName, e).printStackTrace();
            throw e;
        }
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

        AppContextImpl appContext = buildAppContext(appInfo);
        appContext.setAppDir(appDir);
        app.setAppContext(appContext);

        modelInfos.forEach((key, value) -> app.getModelBaseMap().put(value.getCode(), key));

        // 构建 Action 执行器
        modelInfos.forEach((modelBase, modelInfo) -> initActionMap(app, modelInfo.getCode(), modelInfo.getModelActionInfos(), modelBase));
        // 构建 Action 执行器
        Map<String, Collection<ModelActionInfo>> addPresetAction = addPresetAction(modelInfos);// 追加系统预置的 action
        addPresetAction.forEach((modelName, addedModelActions) -> {
            final ModelBase[] modelBase = new ModelBase[1];
            modelInfos.entrySet().stream().filter(entry -> entry.getValue().getCode().equals(modelName)).findAny().ifPresent(entry -> modelBase[0] = entry.getKey());
            PresetAction presetAction = new PresetAction(app, modelBase[0]);
            initActionMap(app, modelName, addedModelActions.toArray(new ModelActionInfo[0]), presetAction);
        });

        return app;
    }

    private void initActionMap(AppImpl app, String modelName, ModelActionInfo[] modelActionInfos, Object instance) {
        Arrays.stream(modelActionInfos).forEach(modelActionInfo -> {
            Map<String, ActionMethod> actionMap = app.getActionMap().computeIfAbsent(modelName, s -> new HashMap<>());
            actionMap.computeIfAbsent(modelActionInfo.getCode(), s -> buildActionMethod(instance, s));
        });
    }

    private ActionMethod buildActionMethod(Object instance, String methodName) {
        return new ActionMethod() {
            private final Method method;

            {
                try {
                    method = instance.getClass().getMethod(methodName, Request.class, Response.class);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void invoke(Request request, Response response) throws Exception {
                method.invoke(instance, request, response);
            }
        };
    }

    private Map<String, Collection<ModelActionInfo>> addPresetAction(Map<ModelBase, ModelInfo> modelSelfInfos) {
        Map<String, Collection<ModelActionInfo>> addedModelActions = new HashMap<>();

        for (Map.Entry<ModelBase, ModelInfo> entry : modelSelfInfos.entrySet()) {
            List<ModelActionInfo> added = new ArrayList<>();

            Set<ModelActionInfo> actions = Arrays.stream(entry.getValue().getModelActionInfos()).collect(Collectors.toSet());
            ModelBase modelBase = entry.getKey();

            // 1. 添加预设的 Action
            Arrays.stream(modelBase.getClass().getInterfaces()).filter(aClass -> aClass.getPackage() == Showable.class.getPackage()).distinct().flatMap((Function<Class<?>, Stream<String>>) aClass -> Arrays.stream(aClass.getFields()).filter(field -> field.getName().startsWith("ACTION_NAME_")).map(field -> {
                try {
                    return field.get(null).toString();
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            })).forEach(addAction -> {
                ModelActionInfo actionInfo = null;
                for (ModelActionInfo action : actions) {
                    if (addAction.equals(action.getCode())) {
                        actionInfo = action;
                        break;
                    }
                }

                for (Map.Entry<Method, ModelActionInfo> ma : presetMethodActionInfos.entrySet()) {
                    if (addAction.equals(ma.getValue().getCode())) {
                        if (actionInfo == null) {
                            added.add(ma.getValue());
                        }
                        break;
                    }
                }
            });

            addedModelActions.put(entry.getValue().getCode(), added);
        }

        for (Map.Entry<String, Collection<ModelActionInfo>> addedModelActionInfos : addedModelActions.entrySet()) {
            ModelInfo addedToModel = null;
            for (ModelInfo value : modelSelfInfos.values()) {
                if (value.getCode().equals(addedModelActionInfos.getKey())) {
                    addedToModel = value;
                    break;
                }
            }
            if (addedToModel != null) {
                List<ModelActionInfo> modelActionInfoList = new ArrayList<>(Arrays.asList(addedToModel.getModelActionInfos()));
                modelActionInfoList.addAll(addedModelActionInfos.getValue());
                addedToModel.setModelActionInfos(modelActionInfoList.toArray(new ModelActionInfo[0]));
            }
        }

        return addedModelActions;
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
            modelInfo.setModelFieldInfos(getModelFieldInfos(annotation, instance));
            modelInfo.setModelActionInfos(parseModelActionInfos(annotation).values().toArray(new ModelActionInfo[0]));
            modelInfo.setGroupInfos(getGroupInfo(instance));
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
        Groups groups = instance.groups();
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
            modelFieldInfo.setPort(modelField.port());
            modelFieldInfo.setUnsupportedCharacters(modelField.unsupportedCharacters());
            modelFieldInfo.setUnsupportedStrings(modelField.unsupportedStrings());
            modelFieldInfo.setShow(modelField.show());
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

    private AppContextImpl buildAppContext(AppInfo appInfo) {
        AppContextImpl appContext = new AppContextImpl(moduleContext, appInfo);
        //        appContext.addActionFilter(new UniqueFilter(appContext));
        return appContext;
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
            scanAppFilesCache.add(appFile);
        }

        addLib(appDir, libs);
        addConfig(appDir, libs);

        if (!isSystemApp) {
            File[] commonFiles = Utils.newFile(moduleContext.getLibDir(), "module", "qingzhou-deployer", "common").listFiles();
            if (commonFiles != null && commonFiles.length > 0) {
                List<File> commonModels = Arrays.asList(commonFiles);
                libs.addAll(commonModels);

                scanAppFilesCache.addAll(commonModels);
            }
        }

        return libs.toArray(new File[0]);
    }

    private void addConfig(File appDir, List<File> libs) {
        File config = Utils.newFile(appDir, "config");
        if (!config.exists()) {
            return;
        }
        libs.add(config);
    }

    private void addLib(File appDir, List<File> libs) {
        File lib = Utils.newFile(appDir, "lib");
        if (!lib.exists()) {
            return;
        }
        for (String fileName : lib.list()) {
            libs.add(new File(lib, fileName));
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
