package qingzhou.core.registry;

import java.io.Serializable;
import java.lang.reflect.Method;

import qingzhou.api.ActionType;

public class ModelActionInfo implements Serializable {
    private transient Method method;
    private String code;
    private String[] name;
    private String[] info;
    private String icon;
    private boolean distribute;
    private boolean requestBody;
    private String display;
    private boolean loginFree;
    private boolean showAction;
    private boolean listAction;
    private boolean headAction;
    private boolean batchAction;
    private boolean formAction;
    private String order;
    private String appPage;
    private String[] subFormFields;
    private boolean subFormAutoload;
    private boolean subFormAutoclose;
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

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public boolean isLoginFree() {
        return loginFree;
    }

    public void setLoginFree(boolean loginFree) {
        this.loginFree = loginFree;
    }

    public boolean isShowAction() {
        return showAction;
    }

    public void setShowAction(boolean showAction) {
        this.showAction = showAction;
    }

    public boolean isListAction() {
        return listAction;
    }

    public void setListAction(boolean listAction) {
        this.listAction = listAction;
    }

    public boolean isHeadAction() {
        return headAction;
    }

    public void setHeadAction(boolean headAction) {
        this.headAction = headAction;
    }

    public boolean isBatchAction() {
        return batchAction;
    }

    public void setBatchAction(boolean batchAction) {
        this.batchAction = batchAction;
    }

    public boolean isFormAction() {
        return formAction;
    }

    public void setFormAction(boolean formAction) {
        this.formAction = formAction;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public boolean isDistribute() {
        return distribute;
    }

    public void setDistribute(boolean distribute) {
        this.distribute = distribute;
    }

    public boolean isRequestBody() {
        return requestBody;
    }

    public void setRequestBody(boolean requestBody) {
        this.requestBody = requestBody;
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

    public boolean isSubFormAutoload() {
        return subFormAutoload;
    }

    public void setSubFormAutoload(boolean subFormAutoload) {
        this.subFormAutoload = subFormAutoload;
    }

    public boolean isSubFormAutoclose() {
        return subFormAutoclose;
    }

    public void setSubFormAutoclose(boolean subFormAutoclose) {
        this.subFormAutoclose = subFormAutoclose;
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
