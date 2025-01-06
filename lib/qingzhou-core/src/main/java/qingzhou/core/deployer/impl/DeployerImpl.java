package qingzhou.core.deployer.impl;

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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import qingzhou.api.*;
import qingzhou.api.type.List;
import qingzhou.api.type.*;
import qingzhou.core.DeployerConstants;
import qingzhou.core.ItemData;
import qingzhou.core.deployer.AppListener;
import qingzhou.core.deployer.AppManager;
import qingzhou.core.deployer.Deployer;
import qingzhou.core.deployer.QingzhouSystemApp;
import qingzhou.core.registry.*;
import qingzhou.engine.ModuleContext;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.logger.Logger;

class DeployerImpl implements Deployer {
    // 同 qingzhou.registry.impl.RegistryImpl.registryInfo 使用自然排序，以支持分页
    private final Map<String, AppManagerImpl> apps = new ConcurrentSkipListMap<>();

    private final ModuleContext moduleContext;
    private final Registry registry;
    private LoaderPolicy loaderPolicy;
    File appsBase = null;
    File systemApp = null;

    private final java.util.List<AppListener> appListeners = new CopyOnWriteArrayList<>();

    DeployerImpl(ModuleContext moduleContext, Registry registry) {
        this.moduleContext = moduleContext;
        this.registry = registry;
    }

    @Override
    public void addAppListener(AppListener appListener) {
        appListeners.add(appListener);
    }

    @Override
    public void removeAppListener(AppListener appListener) {
        appListeners.remove(appListener);
    }

    @Override
    public String installApp(File appDir, Properties deploymentProperties) throws Throwable {
        if (!appDir.isDirectory()) throw new IllegalArgumentException("The app file must be a directory");
        return reBuildAndCacheApp(appDir, deploymentProperties, true);
    }

    private String reBuildAndCacheApp(File appDir, Properties deploymentProperties, boolean withAppInstall) throws Throwable {
        AppManagerImpl app = buildApp(appDir, deploymentProperties);

        if (withAppInstall) {
            try {
                app.getQingzhouApp().install(app.getAppContext());
            } catch (Exception e) {
                destroyApp(app);
            }
            moduleContext.getService(Logger.class).info("The app has been successfully installed: " + appDir.getName());
        }

        // 注册完成
        String name = app.getAppInfo().getName();
        apps.put(name, app);
        return name;
    }

    private void startModel(AppManagerImpl app) throws Exception {
        Field appContextField = Arrays.stream(ModelBase.class.getDeclaredFields())
                .filter(field -> field.getType() == AppContext.class)
                .findFirst().orElseThrow((Supplier<Exception>) () -> new IllegalStateException("not found: " + app.getAppInfo().getName()));
        appContextField.setAccessible(true);

        for (ModelBase modelBase : app.getModelBaseMap().values()) {
            try {
                appContextField.set(modelBase, app.getAppContext());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            modelBase.start();
        }
    }

    @Override
    public void unInstallApp(String appName) {
        AppManagerImpl removed = apps.remove(appName);
        if (removed == null) return;

        removed.getQingzhouApp().uninstall(removed.getAppContext());
        moduleContext.getService(Logger.class).info("The app has been successfully uninstalled: " + appName);
    }

    @Override
    public void startApp(String appName) throws Throwable {
        AppManagerImpl app = apps.get(appName);
        if (app == null) return;
        File appDir = new File(app.getAppInfo().getFilePath());
        reStartApp(appDir);
    }

    public void reStartApp(File file) throws Throwable {
        String appName = reBuildAndCacheApp(file, null, false);
        AppManagerImpl app = apps.get(appName);

        // 加载应用
        Utils.doInThreadContextClassLoader(app.getAppLoader(), () -> {
            app.getQingzhouApp().start(app.getAppContext());
            startModel(app); // 启动应用内的模块
            app.getAppInfo().setState(AppState.Started);
        });

        // 通知启动消息
        appListeners.forEach(appListener -> appListener.onAppStarted(appName));

        moduleContext.getService(Logger.class).info("The app has been successfully started: " + appName);
    }

    @Override
    public void stopApp(String appName) {
        AppManagerImpl app = apps.get(appName);
        if (app == null) return;

        destroyApp(app);

        // 通知停止消息
        appListeners.forEach(appListener -> appListener.onAppStopped(appName));

        moduleContext.getService(Logger.class).info("The app has been successfully stopped: " + appName);
    }

    private void destroyApp(AppManagerImpl app) {
        app.getModelBaseMap().values().forEach(ModelBase::stop);
        app.getQingzhouApp().stop(app.getAppContext());
        app.getAppInfo().setState(AppState.Stopped);

        try {
            app.getAppLoader().close();
        } catch (Exception ignored) {
        }
    }

    @Override
    public java.util.List<String> getLocalApps() {
        return new ArrayList<>(apps.keySet());
    }

    @Override
    public AppManager getApp(String appName) {
        return apps.get(appName);
    }

    @Override
    public java.util.List<String> getAllApp() {
        java.util.List<String> localApps = getLocalApps();
        localApps.addAll(registry.getAllAppNames());
        return localApps;
    }

    @Override
    public AppInfo getAppInfo(String appName) {
        // 优先找本地，master和instance都在本地
        AppManager appManager = apps.get(appName);
        if (appManager != null) return appManager.getAppInfo();

        // 再找远程
        return registry.getAppInfo(appName);
    }

    private AppManagerImpl buildApp(File appDir, Properties deploymentProperties) throws Throwable {
        AppManagerImpl app = new AppManagerImpl(moduleContext);
        app.setAppDir(appDir);

        java.util.List<File> scanAppLibFiles = new ArrayList<>();
        // 1. 只将“根目录”下的 jar 文件加入应用加载路径
        Arrays.stream(Objects.requireNonNull(appDir.listFiles())).filter(f -> f.getName().endsWith(".jar")).forEach(scanAppLibFiles::add);
        // 2. 探测“根目录/lib” 下所有的 jar 文件加入应用加载路径
        appendClassPath(new File(appDir, "lib"), scanAppLibFiles);
        // 3. 探测“根目录/config” 下所有的 jar 文件加入应用加载路径。SpringBoot 等应用实践会将需要加载的资源文件放入此名称目录
        appendClassPath(new File(appDir, "config"), scanAppLibFiles);
        // 3. 轻舟为应用扩展的 jar 文件加入应用加载路径
        File[] additionalLib = loaderPolicy.getAdditionalLib();
        if (additionalLib != null) scanAppLibFiles.addAll(Arrays.asList(additionalLib));

        File[] appLibs = scanAppLibFiles.toArray(new File[0]);
        URLClassLoader loader = buildLoader(appLibs);
        app.setAppLoader(loader);

        try {
            // 解析应用的配置文件
            app.setAppProperties(buildAppProperties(appDir, deploymentProperties));

            QingzhouApp qingzhouApp = buildQingzhouApp(appLibs, loader, app.getAppProperties());
            if (qingzhouApp instanceof QingzhouSystemApp) {
                QingzhouSystemApp qingzhouSystemApp = (QingzhouSystemApp) qingzhouApp;
                qingzhouSystemApp.setModuleContext(moduleContext);
            }
            app.setQingzhouApp(qingzhouApp);

            AppInfo appInfo = new AppInfo();
            String appId = systemApp.equals(appDir) ? DeployerConstants.APP_MASTER : UUID.randomUUID().toString().replace("-", "");
            appInfo.setId(appId);
            appInfo.setName(appDir.getName());
            appInfo.setFilePath(appDir.getAbsolutePath());
            appInfo.setDeploymentProperties(app.getAppProperties());
            Map<ModelBase, ModelInfo> modelInfos = getModelInfos(appLibs, loader, app.getAppProperties());
            modelInfos.values().forEach(appInfo::addModelInfo);
            app.setAppInfo(appInfo);
            app.setAppContext(new AppContextImpl(app));

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
        } catch (Throwable e) {
            loader.close();
            throw e;
        }

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

    private Map<ModelBase, ModelInfo> getModelInfos(File[] appLibs, URLClassLoader loader, Properties appProperties) throws Throwable {
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

        LinkedHashMap<String, ItemData[]> infoList = new LinkedHashMap<>();

        java.util.List<String> staticOptionFields = new ArrayList<>();
        java.util.List<String> dynamicOptionFields = new ArrayList<>();
        for (ModelFieldInfo modelFieldInfo : modelInfo.getModelFieldInfos()) {
            if (modelFieldInfo.isStaticOption()) staticOptionFields.add(modelFieldInfo.getCode());
            if (modelFieldInfo.isDynamicOption()) dynamicOptionFields.add(modelFieldInfo.getCode());
        }
        if (!staticOptionFields.isEmpty()) {
            modelInfo.setStaticOptionFields(staticOptionFields.toArray(new String[0]));
            for (String fieldName : staticOptionFields) {
                Item[] items = option.optionData(null, fieldName);
                if (items != null) {
                    infoList.put(fieldName, Arrays.stream(items).map(ItemData::new).toArray(ItemData[]::new));
                }
            }
        }
        modelInfo.setOptionInfos(infoList);

        modelInfo.setDynamicOptionFields(dynamicOptionFields.toArray(new String[0]));
    }

    private ItemData[] getGroupInfo(ModelBase instance) {
        java.util.List<ItemData> infoList = new ArrayList<>();
        if (instance instanceof Group) {
            Item[] items = ((Group) instance).groupData();
            if (items != null) {
                Arrays.stream(items).map(ItemData::new).forEach(infoList::add);
            }
        }
        return infoList.toArray(new ItemData[0]);
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
            modelFieldInfo.setId(modelField.id());
            modelFieldInfo.setIdMask(modelField.idMask());
            modelFieldInfo.setMin(modelField.min());
            modelFieldInfo.setMax(modelField.max());
            modelFieldInfo.setLengthMin(modelField.min_length());
            modelFieldInfo.setLengthMax(modelField.max_length());
            modelFieldInfo.setPattern(modelField.pattern());
            modelFieldInfo.setHost(modelField.host());
            modelFieldInfo.setPort(modelField.port());
            modelFieldInfo.setPlainText(modelField.plain_text());
            modelFieldInfo.setPlaceholder(modelField.placeholder());
            modelFieldInfo.setSearchMultiple(modelField.search_multiple());
            modelFieldInfo.setReadonly(modelField.readonly());
            modelFieldInfo.setForbid(modelField.forbid());
            modelFieldInfo.setXssSkip(modelField.xss_skip());
            modelFieldInfo.setEmail(modelField.email());
            modelFieldInfo.setFile(modelField.file());
            modelFieldInfo.setLinkModel(modelField.link_model());
            modelFieldInfo.setRefModelClass(modelField.ref_model());
            modelFieldInfo.setActionType(modelField.action_type());
            modelFieldInfo.setColor(modelField.color());
            modelFieldInfo.setEchoGroup(modelField.echo_group());
            modelFieldInfo.setSkipValidate(modelField.skip_validate());
            modelFieldInfo.setStaticOption(modelField.static_option());
            modelFieldInfo.setDynamicOption(modelField.dynamic_option());
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

    private QingzhouApp buildQingzhouApp(File[] appLibs, URLClassLoader loader, Properties appProperties) throws Throwable {
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

    private Properties buildAppProperties(File appDir, Properties deploymentProperties) throws Exception {
        if (!appDir.isDirectory()) throw new IllegalArgumentException("A directory file is required: " + appDir);

        Properties result = null;
        File preferablyFile = new File(appDir, DeployerConstants.QINGZHOU_PROPERTIES_FILE);
        try {
            if (preferablyFile.exists()) {
                try (InputStream in = Files.newInputStream(preferablyFile.toPath())) {
                    result = Utils.streamToProperties(in);
                }
            }

            if (result == null) {
                File[] listFiles = appDir.listFiles();
                if (listFiles != null) {
                    for (File jarFile : listFiles) {
                        if (jarFile.getName().endsWith(".jar")) {
                            Properties properties = Utils.zipEntryToProperties(jarFile, DeployerConstants.QINGZHOU_PROPERTIES_FILE);
                            if (properties != null) {
                                result = properties;
                                break;
                            }
                        }
                    }
                }
            }
        } finally {
            if (deploymentProperties != null) {
                if (result == null) result = new Properties();
                result.putAll(deploymentProperties);

                // 回写入文件
                FileUtil.writeFile(preferablyFile, result);
            }
        }
        return result;
    }

    private void appendClassPath(File path, java.util.List<File> libs) throws IOException {
        if (path.isFile()) {
            libs.add(path);
            if (path.getName().endsWith(".jar")) {
                libs.addAll(parseManifestLib(path));
            }
            return;
        }

        if (path.isDirectory()) {
            File[] listFiles = path.listFiles();
            if (listFiles != null) {
                for (File f : listFiles) {
                    appendClassPath(f, libs);
                }
            }
        }
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
            modelActionInfo.setRequestBody(modelAction.request_body());
            modelActionInfo.setAuthFree(modelAction.auth_free());
            modelActionInfo.setDisplay(modelAction.display());
            modelActionInfo.setShowAction(modelAction.show_action());
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
