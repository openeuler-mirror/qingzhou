package qingzhou.deployer.impl;

import qingzhou.api.AppContext;
import qingzhou.api.Groups;
import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.QingzhouApp;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.Showable;
import qingzhou.deployer.App;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.Utils;
import qingzhou.registry.AppInfo;
import qingzhou.registry.GroupInfo;
import qingzhou.registry.ModelActionInfo;
import qingzhou.registry.ModelFieldInfo;
import qingzhou.registry.ModelInfo;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
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
    public void installApp(File appFile) throws Exception {
        String appName = appFile.getName();
        if (apps.containsKey(appName)) {
            throw new IllegalArgumentException("The app already exists: " + appName);
        }
        boolean isSystemApp = "master".equals(appName) || "instance".equals(appName);
        AppImpl app = buildApp(appName, appFile, isSystemApp);

        // 启动应用
        startApp(app);

        // 初始化各模块
        startModel(app);

        // 注册完成
        apps.put(appName, app);
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

        List<File> qzLibList = new ArrayList<>();
        File[] dependLibs = buildLib(appDir, isSystemApp, qzLibList);
        if (dependLibs.length == 0) {
            throw new IllegalArgumentException(String.format("The jar that implements the Qingzhou App API was not found in the root directory of app [%s]", appName));
        }
        URLClassLoader loader = buildLoader(dependLibs, isSystemApp);
        app.setLoader(loader);

        File[] qzLibs = qzLibList.toArray(new File[0]);
        QingzhouApp qingzhouApp = buildQingzhouApp(qzLibs, loader);
        if (qingzhouApp instanceof QingzhouSystemApp) {
            QingzhouSystemApp qingzhouSystemApp = (QingzhouSystemApp) qingzhouApp;
            qingzhouSystemApp.setModuleContext(moduleContext);
            qingzhouSystemApp.setDeployer(this);
        }
        app.setQingzhouApp(qingzhouApp);

        AppInfo appInfo = new AppInfo();
        appInfo.setName(appName);
        Map<ModelBase, ModelInfo> modelInfos = getModelInfos(qzLibs, loader);
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
            modelFieldInfo.setRefModel(modelField.refModel());
            modelFieldInfo.setDefaultValue(getDefaultValue(field, instance));
            modelFieldInfo.setList(modelField.list());
            modelFieldInfo.setMonitor(modelField.monitor());
            modelFieldInfo.setNumeric(modelField.numeric());
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
        for (File file : appLibs) {
            if (!file.getName().endsWith(".jar")) continue;
            try (JarFile jar = new JarFile(file)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String entryName = entries.nextElement().getName();
                    if (entryName.contains("$") || !entryName.endsWith(".class")) continue;
                    int i = entryName.indexOf(".class");
                    String className = entryName.substring(0, i).replace("/", ".");
                    Class<?> cls = loader.loadClass(className);
                    if (cls.getDeclaredAnnotation(qingzhou.api.App.class) != null) {
                        return (QingzhouApp) cls.newInstance();
                    }
                }
            }
        }

        throw new IllegalStateException("The main class of the app is missing");
    }

    private AppContextImpl buildAppContext(AppInfo appInfo) {
        AppContextImpl appContext = new AppContextImpl(moduleContext, appInfo);
        //        appContext.addActionFilter(new UniqueFilter(appContext));
        appContext.setDefaultDataStore(null);// todo DefaultDataStore 这个设计不够好，设置默认的文件数据源？
        return appContext;
    }

    private File[] buildLib(File appDir, boolean isSystemApp, List<File> qzLibs) throws IOException {
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
            qzLibs.add(appFile);
            libs.add(appFile);
            parseAppFile(appFile, libs);
        }

        if (!isSystemApp) {
            File[] commonFiles = Utils.newFile(moduleContext.getLibDir(), "module", "qingzhou-deployer", "common").listFiles();
            if (commonFiles != null && commonFiles.length > 0) {
                libs.addAll(Arrays.asList(commonFiles));
            }
        }

        return libs.toArray(new File[0]);
    }

    private void parseAppFile(File appFile, List<File> libs) throws IOException {
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
