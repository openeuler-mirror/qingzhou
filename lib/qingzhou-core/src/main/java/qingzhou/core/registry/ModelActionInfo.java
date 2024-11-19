package qingzhou.core.registry;

import qingzhou.api.ActionType;

import java.io.Serializable;
import java.lang.reflect.Method;

public class ModelActionInfo implements Serializable {
    private transient Method method;
    private String code;
    private String[] name;
    private String[] info;
    private String icon;
    private boolean distribute;
    private String show;
    private String appPage;
    private String[] subFormFields;
    private boolean subFormSubmitOnOpen;
    private String[] subMenuModels;
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

    public String[] getSubFormFields() {
        return subFormFields;
    }

    public void setSubFormFields(String[] subFormFields) {
        this.subFormFields = subFormFields;
    }

    public boolean isSubFormSubmitOnOpen() {
        return subFormSubmitOnOpen;
    }

    public void setSubFormSubmitOnOpen(boolean subFormSubmitOnOpen) {
        this.subFormSubmitOnOpen = subFormSubmitOnOpen;
    }

    public String[] getSubMenuModels() {
        return subMenuModels;
    }

    public void setSubMenuModels(String[] subMenuModels) {
        this.subMenuModels = subMenuModels;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }
}
