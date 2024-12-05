package qingzhou.console.controller.rest;

import qingzhou.config.Role;
import qingzhou.config.User;
import qingzhou.console.controller.SystemController;
import qingzhou.console.login.LoginManager;
import qingzhou.core.DeployerConstants;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.registry.ModelActionInfo;
import qingzhou.core.registry.ModelInfo;
import qingzhou.engine.util.pattern.Filter;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SecurityController implements Filter<RestContext> {
    public static boolean isActionPermitted(String app, String model, String action, HttpServletRequest request) {
        // model 是否存在
        ModelInfo modelInfo = SystemController.getModelInfo(app, model);
        if (modelInfo == null) return false;

        // action 是否存在
        ModelActionInfo actionInfo = modelInfo.getModelActionInfo(action);
        if (actionInfo == null) return false;

        // 检查用户的权限
        User currentUser = LoginManager.getLoggedUser(request.getSession(false));
        if (currentUser == null) return false;

        //超管直接放行
        if (DeployerConstants.QINGZHOU_MANAGER_USER_TYP.equals(currentUser.getType())) {
            return true;
        }

        String roles = currentUser.getRole();
        if (roles == null) return false;

        String checkUri = model + "/" + action;
        for (String r : roles.split(DeployerConstants.USER_ROLE_SP)) {
            Role role = SystemController.getConsole().getRole(r);
            String roleApp = role.getApp();
            if (!roleApp.equals(app)) continue;
            String[] roleUris = role.getUris().split(DeployerConstants.ROLE_URI_SP);
            if (Arrays.asList(roleUris).contains(checkUri)) {
                return true;
            }
        }
        return false;
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
        return isActionPermitted(request.getApp(), request.getModel(), request.getAction(), context.req);
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
