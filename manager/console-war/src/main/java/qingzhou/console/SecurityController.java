package qingzhou.console;

import qingzhou.console.controller.SystemController;
import qingzhou.registry.ModelActionInfo;
import qingzhou.registry.ModelInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SecurityController {
    public static boolean isActionPermitted(String app, String model, String action, String user) {
        // model 是否存在
        ModelInfo modelInfo = SystemController.getModelInfo(app, model);
        if (modelInfo == null) return false;

        // action 是否存在
        ModelActionInfo actionInfo = modelInfo.getModelActionInfo(action);
        if (actionInfo == null) return false;

        // 检查用户的权限
        return user != null;
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
