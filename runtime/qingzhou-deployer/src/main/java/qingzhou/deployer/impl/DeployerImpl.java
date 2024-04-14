package qingzhou.deployer.impl;

import qingzhou.api.*;
import qingzhou.api.type.Showable;
import qingzhou.deployer.App;
import qingzhou.deployer.Deployer;
import qingzhou.deployer.QingzhouSystemApp;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;
import qingzhou.registry.*;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

 class DeployerImpl implements Deployer {
    private final Map<Method, ModelAction> presetActions = new AnnotationReader(PresetAction.class).readModelAction();

    private final Map<String, App> apps = new HashMap<>();
    private final ModuleContext moduleContext;

     DeployerImpl(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    @Override
    public void installApp(File appFile) throws Exception {
        String appName = appFile.getName();
        if (apps.containsKey(appName)) {
            throw new IllegalArgumentException("The app already exists: " + appName);
        }
        boolean needCommonModel = !App.SYS_APP_MASTER.equals(appName) && !App.SYS_APP_NODE_AGENT.equals(appName);
        AppImpl app = buildApp(appName, appFile, needCommonModel);
        apps.put(appName, app);

        // 初始化每个 Model
        initActionMethod(app);

        // 启动应用
        app.getQingzhouApp().start(app.getAppContext());
    }

    private void initActionMethod(AppImpl app) {
        for (ModelInfo modelInfo : app.getAppInfo().getModelInfos()) {
            ModelBase modelInstance = app.getModelInstance(modelInfo.getName());
            if (!ModelBase.class.isAssignableFrom(cls)) {
                throw new IllegalArgumentException("The class annotated by the @Model ( " + cls.getName() + " ) needs to 'extends ModelBase'.");
            }
            modelBaseMap.computeIfAbsent(modelName, s -> {
                try {
                    return (ModelBase) cls.newInstance();
                } catch (Exception e) {
                    throw new IllegalArgumentException("The class annotated by the @Model needs to have a public parameter-free constructor.", e);
                }
            });

            modelInstance.setAppContext(app.getAppContext());
            modelInstance.init();
        }

        // 1. 添加预设的 Action
        Arrays.stream(cls.getInterfaces()).filter(aClass -> aClass.getPackage() == Showable.class.getPackage()).distinct().flatMap((Function<Class<?>, Stream<String>>) aClass -> Arrays.stream(aClass.getFields()).filter(field -> field.getName().startsWith("ACTION_NAME_")).map(field -> {
            try {
                return field.get(null).toString();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        })).forEach(actionName -> {
            for (Map.Entry<Method, ModelAction> entry : presetActions.entrySet()) {
                ModelAction modelAction = entry.getValue();
                if (modelAction.name().equals(actionName)) {
                    ActionInfo actionInfo = new ActionInfo(
                            ModelUtil.toModelActionData(modelAction),
                            actionName,
                            new ModelManagerImpl.InvokeMethodImpl(actionMethod, entry.getKey()));
                    actionInfos.put(actionName, actionInfo);
                    break;
                }
            }
        });

        // 2. 添加 Mode 自定义的 Action
        Map<Method, ModelAction> clsActions = annotation.readModelAction(cls);
        for (Map.Entry<Method, ModelAction> entry : clsActions.entrySet()) {
            ModelAction modelAction = entry.getValue();
            String actionName = modelAction.name();
            ActionInfo.InvokeMethod invokeMethod = new ModelManagerImpl.InvokeMethodImpl(instance, entry.getKey());
            ModelActionDataImpl modelActionData = ModelUtil.toModelActionData(modelAction);
            actionInfos.put(actionName, new ActionInfo(modelActionData, actionName, invokeMethod));
        }

        return actionInfos;
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
        } catch (IOException e) {
            Controller.logger.warn("failed to close loader: " + appName, e);
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

    private AppImpl buildApp(String appName, File appDir, boolean needCommonModel) throws Exception {
        AppImpl app = new AppImpl();

        File[] appLibs = buildLib(appDir, needCommonModel);
        URLClassLoader loader = buildLoader(appLibs);
        app.setLoader(loader);

        Collection<String> modelClassName = detectModelClass(appLibs);
        ModelInfo[] modelInfos = getModelInfos(modelClassName, loader);

        AppInfo appInfo = new AppInfo();
        appInfo.setName(appName);
        appInfo.setModelInfos(modelInfos);
        app.setAppInfo(appInfo);

        AppContextImpl appContext = buildAppContext(appInfo);
        app.setAppContext(appContext);

        QingzhouApp qingzhouApp = buildQingzhouApp(loader, appLibs);
        if (qingzhouApp instanceof QingzhouSystemApp) {
            QingzhouSystemApp qingzhouSystemApp = (QingzhouSystemApp) qingzhouApp;
            qingzhouSystemApp.setModuleContext(moduleContext);
        }
        app.setQingzhouApp(qingzhouApp);

        return app;
    }

    private Collection<String> detectModelClass(File[] appLibs) throws Exception {
        Collection<String> modelClasses = new HashSet<>();
        // 构造临时类加载器，避免静态块加载影响业务逻辑
        URLClassLoader tempLoader = new URLClassLoader(Arrays.stream(appLibs).map(file -> {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).toArray(URL[]::new), Model.class.getClassLoader());
        for (File file : appLibs) {
            if (!file.getName().endsWith(".jar")) continue;
            try (JarFile jar = new JarFile(file)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry jarEntry = entries.nextElement();
                    String entryName = jarEntry.getName();
                    String endsWithFlag = ".class";
                    if (entryName.contains("$") || !entryName.endsWith(endsWithFlag)) continue;
                    int i = entryName.indexOf(endsWithFlag);
                    String className = entryName.substring(0, i).replace("/", ".");
                    Class<?> cls = tempLoader.loadClass(className);
                    for (Annotation declaredAnnotation : cls.getDeclaredAnnotations()) {
                        if (declaredAnnotation.getClass().getName().equals(Model.class.getName())) {
                            modelClasses.add(className);
                            break;
                        }
                    }
                }
            }
        }
        return modelClasses;
    }

    private ModelInfo[] getModelInfos(Collection<String> modelClassName, URLClassLoader loader) throws Exception {
        Set<ModelInfo> modelInfos = new HashSet<>();

        for (String s : modelClassName) {
            Class<?> aClass = loader.loadClass(s);
            Model model = aClass.getDeclaredAnnotation(Model.class);

            ModelInfo modelInfo = new ModelInfo();
            modelInfo.setName(model.name());
            modelInfo.setIcon(model.icon());
            modelInfo.setNameI18n(model.nameI18n());
            modelInfo.setInfoI18n(model.infoI18n());
            modelInfo.setEntryAction(model.entryAction());
            modelInfo.setShowToMenu(model.showToMenu());
            modelInfo.setMenuName(model.menuName());
            modelInfo.setMenuOrder(model.menuOrder());

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
            modelInfo.setMonitorFieldInfos(getMonitorFieldInfos(annotation));
            modelInfo.setModelActionInfos(getModelActionInfos(annotation));
            modelInfo.setGroupInfos(getGroupInfo(instance));

            boolean added = modelInfos.add(modelInfo);
            if (!added) {
                throw new IllegalStateException("Duplicate model name: " + model.name());
            }
        }

        return modelInfos.toArray(new ModelInfo[0]);
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

    private ModelActionInfo[] getModelActionInfos(AnnotationReader annotation) {
        Set<ModelActionInfo> modelActionInfos = new HashSet<>();
        annotation.readModelAction().forEach((method, modelAction) -> {
            ModelActionInfo modelActionInfo = new ModelActionInfo();
            modelActionInfo.setName(modelAction.name());
            modelActionInfo.setNameI18n(modelAction.nameI18n());
            modelActionInfo.setInfoI18n(modelAction.infoI18n());
            modelActionInfo.setEffectiveWhen(modelAction.effectiveWhen());
            ActionView actionView = method.getAnnotation(ActionView.class);
            if (actionView != null) {
                ActionViewInfo actionViewInfo = new ActionViewInfo();
                actionViewInfo.setIcon(actionView.icon());
                actionViewInfo.setForwardTo(actionView.forwardTo());
                actionViewInfo.setShownOnList(actionView.shownOnList());
                actionViewInfo.setShownOnListHead(actionView.shownOnListHead());
                modelActionInfo.setActionViewInfo(actionViewInfo);
            }
            modelActionInfos.add(modelActionInfo);
        });
        return modelActionInfos.toArray(new ModelActionInfo[0]);
    }

    private MonitorFieldInfo[] getMonitorFieldInfos(AnnotationReader annotation) {
        List<MonitorFieldInfo> monitorFieldInfoList = new ArrayList<>();
        annotation.readMonitorField().forEach((field, monitorField) -> {
            MonitorFieldInfo monitorFieldInfo = new MonitorFieldInfo();
            monitorFieldInfo.setName(field.getName());
            monitorFieldInfo.setNameI18n(monitorField.nameI18n());
            monitorFieldInfo.setInfoI18n(monitorField.infoI18n());
            monitorFieldInfo.setDynamic(monitorField.dynamic());
            monitorFieldInfo.setDynamicMultiple(monitorField.dynamicMultiple());
            monitorFieldInfoList.add(monitorFieldInfo);
        });
        return monitorFieldInfoList.toArray(new MonitorFieldInfo[0]);
    }

    private ModelFieldInfo[] getModelFieldInfos(AnnotationReader annotation, ModelBase instance) {
        List<ModelFieldInfo> modelFieldInfoList = new ArrayList<>();
        annotation.readModelField().forEach((field, modelField) -> {
            ModelFieldInfo modelFieldInfo = new ModelFieldInfo();
            modelFieldInfo.setName(field.getName());
            modelFieldInfo.setNameI18n(modelField.nameI18n());
            modelFieldInfo.setInfoI18n(modelField.infoI18n());
            modelFieldInfo.setShownOnList(modelField.shownOnList());
            modelFieldInfo.setDefaultValue(getDefaultValue(field, instance));
            FieldValidation fieldValidation = field.getAnnotation(FieldValidation.class);
            if (fieldValidation != null) {
                modelFieldInfo.setFieldValidationInfo(getFieldValidationInfo(fieldValidation));
            }
            FieldView fieldView = field.getAnnotation(FieldView.class);
            if (fieldView != null) {
                modelFieldInfo.setFieldViewInfo(getFieldViewInfo(fieldView));
            }
            modelFieldInfoList.add(modelFieldInfo);
        });
        return modelFieldInfoList.toArray(new ModelFieldInfo[0]);
    }

    private FieldViewInfo getFieldViewInfo(FieldView fieldView) {
        FieldViewInfo fieldViewInfo = new FieldViewInfo();
        fieldViewInfo.setGroup(fieldView.group());
        fieldViewInfo.setType(fieldView.type());
        return fieldViewInfo;
    }

    private FieldValidationInfo getFieldValidationInfo(FieldValidation fieldValidation) {
        FieldValidationInfo fieldValidationInfo = new FieldValidationInfo();
        fieldValidationInfo.setRequired(fieldValidation.required());
        fieldValidationInfo.setNumberMin(fieldValidation.numberMin());
        fieldValidationInfo.setNumberMax(fieldValidation.numberMax());
        fieldValidationInfo.setLengthMin(fieldValidation.lengthMin());
        fieldValidationInfo.setNumberMax(fieldValidation.lengthMax());
        fieldValidationInfo.setHostname(fieldValidation.hostname());
        fieldValidationInfo.setPort(fieldValidation.port());
        fieldValidationInfo.setUnsupportedCharacters(fieldValidation.unsupportedCharacters());
        fieldValidationInfo.setUnsupportedStrings(fieldValidation.unsupportedStrings());
        fieldValidationInfo.setCannotAdd(fieldValidation.cannotAdd());
        fieldValidationInfo.setCannotUpdate(fieldValidation.cannotUpdate());
        fieldValidationInfo.setEffectiveWhen(fieldValidation.effectiveWhen());
        return fieldValidationInfo;
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

    private QingzhouApp buildQingzhouApp(URLClassLoader loader, File[] appLibs) throws Exception {
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
        appContext.addI18n("validator.fail", new String[]{"部分数据不合法", "en:Some of the data is not legitimate"});
        appContext.addActionFilter(new UniqueFilter());
        return appContext;
    }

    private File[] buildLib(File appDir, boolean needCommonModel) {
        File[] appFiles = appDir.listFiles();
        if (appFiles == null) {
            throw new IllegalArgumentException("app lib not found: " + appDir.getName());
        }
        File[] appLibs = appFiles;
        if (needCommonModel) {
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

    private URLClassLoader buildLoader(File[] appLibs) {
        URL[] urls = Arrays.stream(appLibs).map(file -> {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).toArray(URL[]::new);
        return new URLClassLoader(urls, QingzhouSystemApp.class.getClassLoader());
    }
}
