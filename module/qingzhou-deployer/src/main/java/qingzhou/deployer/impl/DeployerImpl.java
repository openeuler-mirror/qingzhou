package qingzhou.deployer.impl;

import qingzhou.api.*;
import qingzhou.api.type.Showable;
import qingzhou.crypto.CryptoService;
import qingzhou.deployer.App;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;
import qingzhou.registry.*;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class DeployerImpl implements Deployer {
    private final Map<Method, ModelActionInfo> presetMethodActionInfos;

    private final Map<String, App> apps = new HashMap<>();
    private final CryptoService cryptoService;
    private final ModuleContext moduleContext;

    DeployerImpl(CryptoService cryptoService, ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
        this.cryptoService = cryptoService;
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

    private void startModel(AppImpl app) {
        Field appContextField = Arrays.stream(ModelBase.class.getDeclaredFields()).filter(field -> field.getType() == AppContext.class).findFirst().get();

        app.getModelBases().forEach(modelBase -> {
            try {
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

        File temp = null;

        QingzhouApp qingzhouApp = app.getQingzhouApp();
        if (qingzhouApp != null) {
            temp = app.getAppContext().getTemp();
            qingzhouApp.stop();
        }

        try {
            app.getLoader().close();
        } catch (Exception ignored) {
        }

        if (temp != null) {
            FileUtil.forceDelete(temp);
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

        File[] appLibs = buildLib(appDir, isSystemApp);
        URLClassLoader loader = buildLoader(appLibs, isSystemApp);
        app.setLoader(loader);

        QingzhouApp qingzhouApp = buildQingzhouApp(appLibs, loader);
        if (qingzhouApp instanceof QingzhouSystemApp) {
            QingzhouSystemApp qingzhouSystemApp = (QingzhouSystemApp) qingzhouApp;
            qingzhouSystemApp.setModuleContext(moduleContext);
            qingzhouSystemApp.setDeployer(this);
            qingzhouSystemApp.setCryptoService(cryptoService);
        }
        app.setQingzhouApp(qingzhouApp);

        AppInfo appInfo = new AppInfo();
        appInfo.setName(appName);
        Map<ModelBase, ModelInfo> modelInfos = getModelInfos(appLibs, loader);
        appInfo.setModelInfos(modelInfos.values());
        app.setAppInfo(appInfo);

        AppContextImpl appContext = buildAppContext(appInfo);
        app.setAppContext(appContext);

        app.getModelBases().addAll(modelInfos.keySet());

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

            Set<String> actions = Arrays.stream(entry.getValue().getModelActionInfos()).map(ModelActionInfo::getCode).collect(Collectors.toSet());
            ModelBase modelBase = entry.getKey();

            // 1. 添加预设的 Action
            Arrays.stream(modelBase.getClass().getInterfaces()).filter(aClass -> aClass.getPackage() == Showable.class.getPackage()).distinct().flatMap((Function<Class<?>, Stream<String>>) aClass -> Arrays.stream(aClass.getFields()).filter(field -> field.getName().startsWith("ACTION_NAME_")).map(field -> {
                try {
                    return field.get(null).toString();
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            })).forEach(addAction -> {
                if (!actions.contains(addAction)) {
                    for (Map.Entry<Method, ModelActionInfo> ma : presetMethodActionInfos.entrySet()) {
                        if (addAction.equals(ma.getValue().getCode())) {
                            added.add(ma.getValue());
                        }
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
        Collection<String> modelClassName = FileUtil.detectAnnotatedClass(appLibs, Model.class, null);

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
            modelActionInfo.setCondition(modelAction.condition());
            modelActionInfo.setForward(modelAction.forward());
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
            modelFieldInfo.setDefaultValue(getDefaultValue(field, instance));
            modelFieldInfo.setList(modelField.list());
            modelFieldInfo.setMonitor(modelField.monitor());
            modelFieldInfo.setNumeric(modelField.numeric());
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

    private File[] buildLib(File appDir, boolean isSystemApp) {
        File[] appFiles = appDir.listFiles();
        if (appFiles == null) {
            throw new IllegalArgumentException("app lib not found: " + appDir.getName());
        }
        File[] appLibs = appFiles;
        if (!isSystemApp) {
            File[] commonFiles = FileUtil.newFile(moduleContext.getLibDir(), "module", "qingzhou-deployer", "common").listFiles();
            if (commonFiles != null) {
                int appFileLength = appFiles.length;
                int commonFileLength = commonFiles.length;
                appLibs = new File[appFileLength + commonFileLength];
                System.arraycopy(appFiles, 0, appLibs, 0, appFileLength);
                System.arraycopy(commonFiles, 0, appLibs, appFileLength, commonFileLength);
            }
        }
        return appLibs;
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
