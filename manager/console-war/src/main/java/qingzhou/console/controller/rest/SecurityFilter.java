package qingzhou.console.controller.rest;

import qingzhou.api.Request;
import qingzhou.console.controller.I18n;
import qingzhou.console.controller.SystemController;
import qingzhou.console.login.LoginManager;
import qingzhou.console.view.type.JsonView;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.pattern.Filter;
import qingzhou.registry.ModelActionInfo;
import qingzhou.registry.ModelInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SecurityFilter implements Filter<RestContext> {
    static {
        I18n.addKeyI18n("page.error.permission.deny", new String[]{"对不起，您无权访问该资源", "en:Sorry, you do not have access to this resource"});
        I18n.addKeyI18n("validator.ActionShow.notsupported", new String[]{"不支持%s操作，未满足条件：%s", "en:The %s operation is not supported, the condition is not met: %s"});// The product uses untrusted data to generated HTML pages. 客户端传来的参数不能回显到页面上，以阻止可能的xss攻击
    }

    public static boolean check(String user, String app, String model, String action) {
        return true;// todo
    }

    public static String isActionAvailable(Request request, Map<String, String> data) {
        String app = request.getApp();
        String model = request.getModel();
        String action = request.getAction();

        ModelInfo modelInfo = SystemController.getModelInfo(app, model);
        if (modelInfo == null) return null;

        ModelActionInfo actionInfo = modelInfo.getModelActionInfo(action);
        if (actionInfo == null) return null;

        String condition = actionInfo.getShow();
        boolean isShow = false;
        try {
            isShow = isShow(condition, data::get);
        } catch (Exception ignored) {
        }
        if (!isShow) {
            return String.format(
                    I18n.getKeyI18n("validator.ActionShow.notsupported"),
                    I18n.getModelI18n(app, "model.action." + model + "." + action),
                    condition);
        }
        return null;
    }

    @Override
    public boolean doFilter(RestContext context) throws Exception {
        String user = LoginManager.getLoginUser(context.req.getSession(false));
        if (user != null) {
            if (check(user,
                    context.request.getApp(), context.request.getModel(), context.request.getAction()))
                return true;
        }

        String msg = I18n.getKeyI18n("page.error.permission.deny");
        JsonView.responseErrorJson(context.resp, msg);
        return false;
    }

    public static boolean isShow(String condition, FieldValueRetriever retriever) throws Exception {
        if (Utils.isBlank(condition)) {
            return true;
        }

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
        if (queue == null) {
            return true;
        }

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

    public interface FieldValueRetriever {
        String getFieldValue(String fieldName) throws Exception;
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
