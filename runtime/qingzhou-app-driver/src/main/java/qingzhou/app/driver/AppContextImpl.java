package qingzhou.app.driver;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.*;

import qingzhou.api.ActionFilter;
import qingzhou.api.AppContext;
import qingzhou.api.ModelBase;
import qingzhou.api.Request;
import qingzhou.dto.meta.AppMeta;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelAction;
import qingzhou.logger.Logger;

class AppContextImpl implements AppContext {
    private final AppDriver appDriver;
    private final AppMeta appMeta;
    private final List<ActionFilter> actionFilters = new ArrayList<>();
    private final Map<Model, ModelBase> modelInstances = new HashMap<>();
    private final Map<String, Method> actionMethods = new HashMap<>();
    private File appTemp;
    Properties appProperties;

    AppContextImpl(AppDriver appDriver, AppMeta appMeta) {
        this.appDriver = appDriver;
        this.appMeta = appMeta;
    }

    @Override
    public Properties getProperties() {
        return appProperties;
    }

    @Override
    public File getBase() {
        return appDriver.instanceFile;
    }

    @Override
    public String getVersion() {
        return appDriver.qzVersion;
    }

    @Override
    public void addActionFilter(ActionFilter... actionFilter) {
        actionFilters.addAll(Arrays.asList(actionFilter));
    }

    @Override
    public File getTemp() {
        if (appTemp == null) {
            appTemp = Paths.get(getBase().getAbsolutePath(), "temp", "apps", appMeta.getApp().code).toFile();
        }
        return appTemp;
    }

    @Override
    public <T> T getService(Class<T> clazz) {
        return appDriver.getService(clazz);
    }

    @Override
    public <T> T getService(Class<T> clazz, String name) {
        return appDriver.getService(clazz, name);
    }

    @Override
    public <T> T getObjectInstance(Class<T> type) {
        if (type != null) {
            for (ModelBase modelBase : modelInstances.values()) {
                if (type.isInstance(modelBase)) return (T) modelBase;
            }
            if (type.isInstance(appDriver.qingzhouApp)) {
                return (T) appDriver.qingzhouApp;
            }
        }
        return null;
    }

    public List<ActionFilter> getActionFilters() {
        return actionFilters;
    }

    public Map<String, Method> getActionMethods() {
        return actionMethods;
    }

    public Map<Model, ModelBase> getModelInstances() {
        return modelInstances;
    }

    void startModelInstances() {
        // 初始化模块实例
        appMeta.getApp().models.forEach(model -> {
            try {
                Class<?> modelClass = Class.forName(model.className);
                ModelBase modelBase = (ModelBase) modelClass.newInstance();
                initDefaultValue(model, modelBase);
                modelInstances.put(model, modelBase);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });

        // 初始化模块操作
        appMeta.getApp().models.forEach(model -> model.actions.forEach(action -> {
            ModelBase modelBase = modelInstances.get(model);

            try {
                Method actionMethod = modelBase.getClass().getMethod(action.methodName, Request.class);
                actionMethods.put(resolveActionKey(model, action), actionMethod);
            } catch (Throwable ignored) {
                // 实现的 Show List 等类型的默认方法不只有 Request.class 一个参数
                // 他们在 invokeAction 中被委派到 DefaultAction 中处理
            }
        }));

        // 启动模块
        modelInstances.values().forEach(modelBase -> {
            modelBase.setAppContext(AppContextImpl.this);
            modelBase.start();
        });
    }

    private void initDefaultValue(Model model, ModelBase modelBase) {
        model.fields.forEach(modelField -> {
            try {
                Field field = modelBase.getClass().getField(modelField.code);
                Object val = field.get(modelBase);
                if (val != null) {
                    modelField.default_value = val.toString();
                }
            } catch (Exception ignored) {
                getService(Logger.class).warn("failed to parse default value, model: " + model.code + ", field: " + modelField.code);
            }
        });
    }

    void stopModelInstances() {
        modelInstances.values().forEach(ModelBase::stop);
    }

    static String resolveActionKey(Model model, ModelAction action) {
        return model.code + "->" + action.code;
    }
}
