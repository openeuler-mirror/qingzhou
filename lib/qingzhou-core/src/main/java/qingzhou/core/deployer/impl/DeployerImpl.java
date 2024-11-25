package qingzhou.core.deployer.impl;

import qingzhou.api.*;
import qingzhou.api.type.List;
import qingzhou.api.type.*;
import qingzhou.core.DeployerConstants;
import qingzhou.core.ItemInfo;
import qingzhou.core.deployer.App;
import qingzhou.core.deployer.AppListener;
import qingzhou.core.deployer.Deployer;
import qingzhou.core.deployer.QingzhouSystemApp;
import qingzhou.core.registry.*;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

class DeployerImpl implements Deployer {
    // 同 qingzhou.registry.impl.RegistryImpl.registryInfo 使用自然排序，以支持分页
    private final Map<String, AppImpl> apps = new ConcurrentSkipListMap<>();

    private final ModuleContext moduleContext;
    private final Registry registry;
    private LoaderPolicy loaderPolicy;
    File appsBase = null;

    private final java.util.List<AppListener> appListeners = new ArrayList<>();

    DeployerImpl(ModuleContext moduleContext, Registry registry) {
        this.moduleContext = moduleContext;
        this.registry = registry;
    }

    @Override
    public void addAppListener(AppListener appListener) {
        appListeners.add(appListener);
    }

    @Override
    public void installApp(File appDir) throws Exception {
        if (!appDir.isDirectory()) throw new IllegalArgumentException("The app file must be a directory");

        AppImpl app = buildApp(appDir);

        // 加载应用
        Utils.doInThreadContextClassLoader(app.getLoader(), (Utils.InvokeInThreadContextClassLoader<Void>) () -> {
            startApp(app);
            startModel(app);
            return null;
        });

        // 注册完成
        String name = app.getAppInfo().getName();
        apps.put(name, app);
        appListeners.forEach(appListener -> appListener.onInstalled(name));

        moduleContext.getService(Logger.class).info("The app has been successfully installed: " + appDir.getName());
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
        app.getAppInfo().setState(DeployerConstants.APP_STARTED);
    }

    @Override
    public void unInstallApp(String appName) throws Exception {
        stopApp(appName);
        apps.remove(appName);
    }

    @Override
    public void startApp(String appName) throws Exception {
        File appDir = FileUtil.newFile(appsBase, appName);
        installApp(appDir);
    }

    @Override
    public void stopApp(String appName) throws Exception {
        AppImpl app = apps.get(appName);
        if (app == null) return;

        app.getModelBaseMap().values().forEach(ModelBase::stop);

        QingzhouApp qingzhouApp = app.getQingzhouApp();
        if (qingzhouApp != null) {
            qingzhouApp.stop();
            app.getAppInfo().setState(DeployerConstants.APP_STOPPED);
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
    public App getApp(String appName) {
        return apps.get(appName);
    }

    @Override
    public AppInfo getAppInfo(String appName) {
        // 优先找本地，master和instance都在本地
        App app = getApp(appName);
        if (app != null) return app.getAppInfo();

        // 再找远程
        return registry.getAppInfo(appName);
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

        // 解析应用的配置文件
        Properties appProperties = buildAppProperties(appDir);
        app.setAppProperties(appProperties);

        QingzhouApp qingzhouApp = buildQingzhouApp(appLibs, loader, appProperties);
        if (qingzhouApp instanceof QingzhouSystemApp) {
            QingzhouSystemApp qingzhouSystemApp = (QingzhouSystemApp) qingzhouApp;
            qingzhouSystemApp.setModuleContext(moduleContext);
        }
        app.setQingzhouApp(qingzhouApp);

        AppInfo appInfo = new AppInfo();
        appInfo.setName(appDir.getName());
        appInfo.setFilePath(appDir.getAbsolutePath());
        Map<ModelBase, ModelInfo> modelInfos = getModelInfos(appLibs, loader, appProperties);
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
        Map<String, java.util.List<ModelActionInfo>> addedSuperActions = addSuperAction(modelInfos);// 追加系统预置的 action
        addedSuperActions.forEach((modelName, addedModelActions) -> {
            ModelBase[] findModelBase = new ModelBase[1];
            modelInfos.entrySet().stream().filter(entry -> entry.getValue().getCode().equals(modelName)).findAny().ifPresent(entry -> findModelBase[0] = entry.getKey());
            SuperAction superAction = new SuperAction(app, findModelBase[0]);
            Map<String, ActionMethod> moelMap = app.getModelActionMap().computeIfAbsent(modelName, model -> new HashMap<>());
            for (ModelActionInfo action : addedModelActions) {
                ActionMethod actionMethod = ActionMethod.buildActionMethod(action.getMethod(), superAction);
                moelMap.put(action.getCode(), actionMethod);
            }
        });

        return app;
    }

    private Map<String, java.util.List<ModelActionInfo>> addSuperAction(Map<ModelBase, ModelInfo> modelInfos) {
        Map<String, java.util.List<ModelActionInfo>> addActionToModels = detectActionsToAdd(modelInfos);
        mergeSuperActions(modelInfos, addActionToModels);
        return addActionToModels;
    }

    private Map<String, java.util.List<ModelActionInfo>> detectActionsToAdd(Map<ModelBase, ModelInfo> forModels) {
        Map<String, java.util.List<ModelActionInfo>> addActionToModels = new HashMap<>();
        for (Map.Entry<ModelBase, ModelInfo> entry : forModels.entrySet()) {
            java.util.List<ModelActionInfo> added = new ArrayList<>();
            Set<ModelActionInfo> selfActions = Arrays.stream(entry.getValue().getModelActionInfos()).collect(Collectors.toSet());
            ModelBase modelBase = entry.getKey();
            Set<String> detectedActionNames = new HashSet<>();
            findSuperActions(modelBase.getClass(), detectedActionNames);
            detectedActionNames.forEach(addActionName -> {
                boolean exists = false;
                for (ModelActionInfo self : selfActions) {
                    if (addActionName.equals(self.getCode())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    for (ModelActionInfo actionInfo : SuperAction.ALL_SUPER_ACTION_CACHE) {
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

    private void mergeSuperActions(Map<ModelBase, ModelInfo> modelInfos, Map<String, java.util.List<ModelActionInfo>> addActionToModels) {
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

    private Collection<String> filterClasses(Collection<String> classNames, String filter, boolean match) {
        if (classNames == null || classNames.isEmpty()) return classNames;
        if (Utils.isBlank(filter)) return classNames;

        Set<String> filterPattern = Arrays.stream(filter.split(","))
                .map(String::trim)
                .map(String::toLowerCase).collect(Collectors.toSet());
        return classNames.stream()
                .filter(className -> {
                    if (match) {
                        return filterPattern.stream().anyMatch(className.toLowerCase()::contains);
                    } else {
                        return filterPattern.stream().noneMatch(className.toLowerCase()::contains);
                    }
                }).collect(Collectors.toSet());
    }

    private Map<ModelBase, ModelInfo> getModelInfos(File[] appLibs, URLClassLoader loader, Properties appProperties) throws Exception {
        String filename = null;
        String include = null;
        String exclude = null;
        if (appProperties != null) {
            filename = appProperties.getProperty(DeployerConstants.QINGZHOU_PROPERTIES_APP_SCAN_FILENAME);
            include = appProperties.getProperty(DeployerConstants.QINGZHOU_PROPERTIES_APP_SCAN_INCLUDE);
            exclude = appProperties.getProperty(DeployerConstants.QINGZHOU_PROPERTIES_APP_SCAN_EXCLUDE);
        }
        //filename过滤扫描jar
        if (Utils.notBlank(filename)) {
            Set<String> filenameSet = Arrays.stream(filename.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            File[] additionalLib = loaderPolicy.getAdditionalLib();
            if (additionalLib != null) {
                filenameSet.addAll(Arrays.stream(additionalLib).map(File::getName).collect(Collectors.toSet()));
            }
            appLibs = Arrays.stream(appLibs)
                    .filter(appLib -> filenameSet.stream()
                            .anyMatch(appLib.getName().toLowerCase()::contains))
                    .toArray(File[]::new);
        }

        Collection<String> modelClassName = Utils.detectAnnotatedClass(appLibs, Model.class, loader);
        //include和exclude过滤类
        modelClassName = filterClasses(modelClassName, include, true);
        modelClassName = filterClasses(modelClassName, exclude, false);

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
            initOptionInfo(modelInfo, instance);
            initListInfo(modelInfo, instance);
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
                    modelInfoEntry = modelInfos.entrySet().stream().filter(entry -> refModelClass == entry.getKey().getClass()).findAny().orElseThrow((Supplier<Throwable>) () -> new IllegalArgumentException("Ref-Model-Class " + refModelClass + " not found"));
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                modelFieldInfo.setRefModel(modelInfoEntry.getValue().getCode());
            }
        });

        return modelInfos;
    }

    private void initListInfo(ModelInfo modelInfo, ModelBase instance) {
        if (!(instance instanceof List)) return;

        List listInstance = (List) instance;
        modelInfo.setShowOrderNumber(listInstance.showOrderNumber());
        modelInfo.setUseDynamicDefaultSearch(listInstance.useDynamicDefaultSearch());
        if (!modelInfo.isUseDynamicDefaultSearch()) {
            modelInfo.setDefaultSearch(listInstance.defaultSearch());
        }
    }

    private void initOptionInfo(ModelInfo modelInfo, ModelBase instance) {
        if (!(instance instanceof Option)) return;
        Option option = (Option) instance;

        LinkedHashMap<String, ItemInfo[]> infoList = new LinkedHashMap<>();
        String[] staticOptionFields = option.staticOptionFields();
        if (staticOptionFields != null) {
            modelInfo.setStaticOptionFields(staticOptionFields);
            for (String fieldName : staticOptionFields) {
                Item[] items = option.optionData(fieldName);
                if (items != null) {
                    infoList.put(fieldName, Arrays.stream(items).map(ItemInfo::new).toArray(ItemInfo[]::new));
                }
            }
        }
        modelInfo.setOptionInfos(infoList);

        modelInfo.setDynamicOptionFields(option.dynamicOptionFields());
    }

    private ItemInfo[] getGroupInfo(ModelBase instance) {
        java.util.List<ItemInfo> infoList = new ArrayList<>();
        if (instance instanceof Group) {
            Item[] items = ((Group) instance).groupData();
            if (items != null) {
                Arrays.stream(items).map(ItemInfo::new).forEach(infoList::add);
            }
        }
        return infoList.toArray(new ItemInfo[0]);
    }

    private ModelFieldInfo[] getModelFieldInfos(AnnotationReader annotation, ModelBase instance) {
        java.util.List<ModelFieldInfo> modelFieldInfoList = new ArrayList<>();
        annotation.readModelField().forEach((field, modelField) -> {
            ModelFieldInfo modelFieldInfo = new ModelFieldInfo();
            modelFieldInfo.setCode(field.getName());
            modelFieldInfo.setName(modelField.name());
            modelFieldInfo.setInfo(modelField.info());
            modelFieldInfo.setGroup(modelField.group());
            modelFieldInfo.setInputType(modelField.input_type());
            modelFieldInfo.setSeparator(modelField.separator());
            modelFieldInfo.setDefaultValue(getDefaultValue(field, instance));
            modelFieldInfo.setShow(modelField.show());
            modelFieldInfo.setHidden(modelField.hidden());
            modelFieldInfo.setUpdateAction(modelField.update_action());
            modelFieldInfo.setCreate(modelField.create());
            modelFieldInfo.setEdit(modelField.edit());
            modelFieldInfo.setList(modelField.list());
            modelFieldInfo.setIgnore(modelField.ignore());
            modelFieldInfo.setSearch(modelField.search());
            modelFieldInfo.setLinkAction(modelField.link_action());
            modelFieldInfo.setWidthPercent(modelField.width_percent());
            modelFieldInfo.setFieldType(modelField.field_type());
            modelFieldInfo.setNumeric(modelField.numeric());
            modelFieldInfo.setDisplay(modelField.display());
            modelFieldInfo.setRequired(modelField.required());
            modelFieldInfo.setMin(modelField.min());
            modelFieldInfo.setMax(modelField.max());
            modelFieldInfo.setLengthMin(modelField.min_length());
            modelFieldInfo.setLengthMax(modelField.max_length());
            modelFieldInfo.setPattern(modelField.pattern());
            modelFieldInfo.setHost(modelField.host());
            modelFieldInfo.setPort(modelField.port());
            modelFieldInfo.setPlainText(modelField.plain_text());
            modelFieldInfo.setPlaceholder(modelField.placeholder());
            modelFieldInfo.setMultipleSearch(modelField.multiple_search());
            modelFieldInfo.setReadonly(modelField.readonly());
            modelFieldInfo.setForbid(modelField.forbid());
            modelFieldInfo.setSkip(modelField.skip());
            modelFieldInfo.setEmail(modelField.email());
            modelFieldInfo.setFile(modelField.file());
            modelFieldInfo.setLinkModel(modelField.link_model());
            modelFieldInfo.setRefModelClass(modelField.ref_model());
            modelFieldInfo.setActionType(modelField.action_type());
            modelFieldInfo.setColor(modelField.color());
            modelFieldInfo.setEchoGroup(modelField.echo_group());
            modelFieldInfo.setSkipValidate(modelField.skip_validate());
            modelFieldInfo.setOrder(modelField.order());
            modelFieldInfo.setSameLine(modelField.same_line());
            modelFieldInfo.setShowLabel(modelField.show_label());
            modelFieldInfo.setCombineFields(modelField.combine_fields());
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

    private QingzhouApp buildQingzhouApp(File[] appLibs, URLClassLoader loader, Properties appProperties) throws Exception {
        String mainClass = null;
        if (appProperties != null) {
            mainClass = appProperties.getProperty(DeployerConstants.QINGZHOU_PROPERTIES_APP_MAIN_CLASS);
        }
        if (mainClass == null) {
            Collection<String> annotatedClass = Utils.detectAnnotatedClass(appLibs, qingzhou.api.App.class, loader);
            if (annotatedClass.size() == 1) {
                mainClass = annotatedClass.iterator().next();
            }
        }

        try {
            Class<?> cls = loader.loadClass(mainClass);
            return (QingzhouApp) cls.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("An app must have and can only have one implementation class for " + QingzhouApp.class.getName(), e);
        }
    }

    private Properties buildAppProperties(File appDir) throws Exception {
        if (appDir.isDirectory()) {
            File file = new File(appDir, DeployerConstants.QINGZHOU_PROPERTIES_FILE);
            if (file.exists()) {
                try (InputStream in = Files.newInputStream(file.toPath())) {
                    return Utils.streamToProperties(in);
                }
            }

            File[] listFiles = appDir.listFiles();
            for (File jarFile : Objects.requireNonNull(listFiles)) {
                if (jarFile.getName().endsWith(".jar")) {
                    Properties properties = Utils.zipEntryToProperties(jarFile, DeployerConstants.QINGZHOU_PROPERTIES_FILE);
                    if (properties != null) {
                        return properties;
                    }
                }
            }
        }

        if (appDir.isFile()) {
            return Utils.zipEntryToProperties(appDir, DeployerConstants.QINGZHOU_PROPERTIES_FILE);
        }

        return null;
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
            ModelActionInfo modelActionInfo = new ModelActionInfo();
            modelActionInfo.setMethod(method);
            modelActionInfo.setCode(modelAction.code());
            modelActionInfo.setName(modelAction.name());
            modelActionInfo.setInfo(modelAction.info());
            modelActionInfo.setIcon(modelAction.icon());
            modelActionInfo.setDistribute(modelAction.distribute());
            modelActionInfo.setShow(modelAction.show());
            modelActionInfo.setListAction(modelAction.list_action());
            modelActionInfo.setHeadAction(modelAction.head_action());
            modelActionInfo.setBatchAction(modelAction.batch_action());
            modelActionInfo.setFormAction(modelAction.form_action());
            modelActionInfo.setOrder(modelAction.order());
            modelActionInfo.setAppPage(modelAction.app_page());
            modelActionInfo.setSubFormFields(modelAction.sub_form_fields());
            modelActionInfo.setSubFormAutoload(modelAction.sub_form_autoload());
            modelActionInfo.setSubFormAutoclose(modelAction.sub_form_autoclose());
            modelActionInfo.setSubMenuModels(modelAction.sub_menu_models());
            modelActionInfo.setActionType(modelAction.action_type());
            modelActionInfos.add(modelActionInfo);
        });
        return modelActionInfos;
    }

    static void findSuperActions(Class<?> checkClass, Set<String> superActions) {
        Set<String> foundActions = findSuperActions(checkClass);
        if (foundActions != null) {
            superActions.addAll(foundActions);
        }

        Class<?> superClass = checkClass.getSuperclass();
        if (superClass != null) {
            findSuperActions(superClass, superActions);
        }

        for (Class<?> c : checkClass.getInterfaces()) {
            findSuperActions(c, superActions);
        }
    }

    private static Set<String> findSuperActions(Class<?> checkClass) {
        if (!checkClass.isInterface()) return null;
        if (checkClass.getPackage() != (Add.class.getPackage())) return null;

        Set<String> superActions = new HashSet<>();
        for (Field field : checkClass.getDeclaredFields()) {
            if (field.getName().startsWith("ACTION_")) {
                try {
                    superActions.add((String) field.get(null));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return superActions;
    }

    void setLoaderPolicy(LoaderPolicy loaderPolicy) {
        this.loaderPolicy = loaderPolicy;
    }

    interface LoaderPolicy {
        ClassLoader getClassLoader();

        File[] getAdditionalLib();
    }
}
