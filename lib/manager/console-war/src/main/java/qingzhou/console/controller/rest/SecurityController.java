package qingzhou.console.controller.rest;

import qingzhou.api.MsgLevel;
import qingzhou.config.console.Role;
import qingzhou.config.console.User;
import qingzhou.console.controller.SystemController;
import qingzhou.console.login.LoginManager;
import qingzhou.core.DeployerConstants;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.registry.AppInfo;
import qingzhou.core.registry.ModelActionInfo;
import qingzhou.core.registry.ModelInfo;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class SecurityController implements Filter<RestContext> {
    public static boolean isActionPermitted(String app, String model, String action, HttpServletRequest request, String[] dataListFields, String[] dataValues) {
        Map<String, String> modelData = new HashMap<>();
        for (int i = 0; i < dataListFields.length; i++) {
            modelData.put(dataListFields[i], dataValues[i]);
        }
        return isActionPermitted(app, model, action, request, modelData);
    }

    public static boolean isActionPermitted(String app, String model, String action, HttpServletRequest request) {
        return isActionPermitted(app, model, action, request, null);
    }

    public static boolean isActionPermitted(String app, String model, String action, HttpServletRequest request, Map<String, String> modelData) {
        boolean actionPermitted = isActionPermitted0(app, model, action, request);
        if (actionPermitted && modelData != null) {
            ModelInfo modelInfo = SystemController.getModelInfo(app, model);
            ModelActionInfo actionInfo = Objects.requireNonNull(modelInfo).getModelActionInfo(action);
            String display = actionInfo.getDisplay();
            if (Utils.notBlank(display)) {
                actionPermitted = SecurityController.checkRule(display, modelData::get);
            }
        }
        return actionPermitted;
    }

    private static boolean isActionPermitted0(String app, String model, String action, HttpServletRequest request) {
        AppInfo appInfo = SystemController.getAppInfo(app);

        // app 是否存在
        if (appInfo == null) return false;
        // model 是否存在
        ModelInfo modelInfo = appInfo.getModelInfo(model);
        if (modelInfo == null) return false;
        // action 是否存在
        ModelActionInfo actionInfo = modelInfo.getModelActionInfo(action);
        if (actionInfo == null) return false;

        Set<String> set = appInfo.getAuthFreeModelActions().get(model);
        if (set != null && set.contains(action)) return true;

        // 是否是开放的 model，需要放在 getLoggedUser 之前，远程注册时候没有用户
        if (DeployerConstants.APP_MASTER.equals(app)) {
            if (DeployerConstants.NONE_ROLE_SYSTEM_MODELS.contains(model)) return true;

            Set<String> actions = DeployerConstants.NONE_ROLE_SYSTEM_MODEL_ACTIONS.get(model);
            if (actions != null && actions.contains(actionInfo.getCode())) {
                return true;
            }
        } else {
            if (DeployerConstants.NONE_ROLE_NONE_SYSTEM_MODELS.contains(model)) {
                return true;
            }
        }


        // 检查用户的权限
        User currentUser = LoginManager.getLoggedUser(request.getSession(false));
        if (currentUser == null) return false;

        //超管直接放行
        if (DeployerConstants.QINGZHOU_MANAGER_USER_TYP.equals(currentUser.getType())) {
            return true;
        }

        String roles = currentUser.getRole();
        if (roles == null) return false;

        String checkUri = model + DeployerConstants.MULTISELECT_GROUP_SEPARATOR + action;
        for (String r : roles.split(DeployerConstants.USER_ROLE_SP)) {
            Role role = SystemController.getConsole().getRole(r);
            if (role != null) {
                if (app.equals(DeployerConstants.APP_MASTER)) {
                    if (hasUri(role.getMasterAppUris(), checkUri)) {
                        return true;
                    }
                }

                if (app.equals(role.getApp())) {
                    if (hasUri(role.getUris(), checkUri)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasUri(String uris, String checkUri) {
        String[] roleUris = uris.split(DeployerConstants.ROLE_URI_SP);
        return Arrays.asList(roleUris).contains(checkUri);
    }

    public static boolean checkRule(String condition, FieldValueRetriever retriever) {
        if ("true".equalsIgnoreCase(condition)) return true;
        if ("false".equalsIgnoreCase(condition)) return false;

        AndOrQueue queue = null;
        String[] split;
        if ((split = condition.split("&")).length > 1) {
            queue = new AndOrQueue(true);
        } else if ((split = condition.split("\\|")).length > 1) {
            queue = new AndOrQueue(false);
        }
        if (queue == null) {
            if (split.length > 0) {
                queue = new AndOrQueue(true);
            }
        }
        if (queue == null) return false;

        String notEqStr = "!=";
        String eqStr = "=";
        for (String s : split) {
            int notEq = s.indexOf(notEqStr);
            if (notEq > 1) {
                String f = s.substring(0, notEq);
                String v = s.substring(notEq + notEqStr.length());
                queue.addComparator(new ShowComparator(false, retriever.getFieldValue(f), v));
                continue;
            }
            int eq = s.indexOf(eqStr);
            if (eq > 1) {
                String f = s.substring(0, eq);
                String v = s.substring(eq + eqStr.length());
                queue.addComparator(new ShowComparator(true, retriever.getFieldValue(f), v));
            }
        }

        return queue.compare();
    }

    @Override
    public boolean doFilter(RestContext context) throws Exception {
        RequestImpl request = context.request;
        boolean actionPermitted = isActionPermitted(request.getApp(), request.getModel(), request.getAction(), context.req);
        if (!actionPermitted) {
            context.resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            request.getResponse().setSuccess(false);
            request.getResponse().setMsgLevel(MsgLevel.ERROR);
            request.getResponse().setMsg("The current user has no permissions");
        }
        return actionPermitted;
    }

    public interface FieldValueRetriever {
        String getFieldValue(String fieldName);
    }

    private static final class ShowComparator {
        final boolean eqOrNot;
        final String v1;
        final String v2;

        ShowComparator(boolean eqOrNot, String v1, String v2) {
            this.eqOrNot = eqOrNot;
            this.v1 = v1;
            this.v2 = v2;
        }

        boolean compare() {
            String vv1 = v1;
            String vv2 = v2;
            if (vv1 != null) {
                vv1 = vv1.toLowerCase();
            }
            if (vv2 != null) {
                vv2 = vv2.toLowerCase();
            }
            return eqOrNot == Objects.equals(vv1, vv2);
        }
    }

    private static final class AndOrQueue {
        final boolean andOr;
        final List<ShowComparator> comparators = new ArrayList<>();

        AndOrQueue(boolean andOr) {
            this.andOr = andOr;
        }

        void addComparator(ShowComparator comparator) {
            comparators.add(comparator);
        }

        boolean compare() {
            if (andOr) {
                for (ShowComparator c : comparators) {
                    if (!c.compare()) {
                        return false;
                    }
                }
                return true;
            } else {
                for (ShowComparator c : comparators) {
                    if (c.compare()) {
                        return true;
                    }
                }
                return false;
            }
        }
    }
}
