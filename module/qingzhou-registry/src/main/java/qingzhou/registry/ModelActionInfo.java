package qingzhou.registry;

import qingzhou.api.ActionType;

import java.lang.reflect.Method;

public class ModelActionInfo {
    private transient Method method;
    private String code;
    private String[] name;
    private String[] info;
    private String icon;
    private boolean distribute;
    private String show;
    private String appPage;
    private String[] formFields;
    private boolean subFormSubmitOnOpen;
    private String[] menuModels;
    private ActionType actionType;

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String[] getName() {
        return name;
    }

    public void setName(String[] name) {
        this.name = name;
    }

    public String[] getInfo() {
        return info;
    }

    public void setInfo(String[] info) {
        this.info = info;
    }

    public String getShow() {
        return show;
    }

    public void setShow(String show) {
        this.show = show;
    }

    public boolean isDistribute() {
        return distribute;
    }

    public void setDistribute(boolean distribute) {
        this.distribute = distribute;
    }

    public String getAppPage() {
        return appPage;
    }

    public void setAppPage(String appPage) {
        this.appPage = appPage;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String[] getFormFields() {
        return formFields;
    }

    public void setFormFields(String[] formFields) {
        this.formFields = formFields;
    }

    public boolean isSubFormSubmitOnOpen() {
        return subFormSubmitOnOpen;
    }

    public void setSubFormSubmitOnOpen(boolean subFormSubmitOnOpen) {
        this.subFormSubmitOnOpen = subFormSubmitOnOpen;
    }

    public String[] getMenuModels() {
        return menuModels;
    }

    public void setMenuModels(String[] menuModels) {
        this.menuModels = menuModels;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }
}
