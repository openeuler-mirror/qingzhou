package qingzhou.framework.util;

import qingzhou.framework.AppInfo;
import qingzhou.framework.FrameworkContext;
import qingzhou.framework.api.AppContext;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.Constants;
import qingzhou.framework.impl.FrameworkContextImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ModelUtil { // todo ? 移动到合适的位置
    public static FrameworkContext getFrameworkContext() {
        return FrameworkContextImpl.getFrameworkContext();
    }

    public static AppContext getMasterAppContext() {
        AppInfo appInfo = getFrameworkContext().getAppInfoManager().getAppInfo(Constants.MASTER_APP_NAME);
        return appInfo.getAppContext();
    }

    public static ConsoleContext getMasterConsoleContext() {
        return getMasterAppContext().getConsoleContext();
    }

    public static boolean isEffective(FieldValueRetriever retriever, String effectiveWhen) throws Exception {
        if (StringUtil.isBlank(effectiveWhen)) {
            return true;
        }

        AndOrQueue queue = null;
        String[] split;
        if ((split = effectiveWhen.split("&")).length > 1) {
            queue = new AndOrQueue(true);
        } else if ((split = effectiveWhen.split("\\|")).length > 1) {
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
                queue.addComparator(new Comparator(false, retriever.getFieldValue(f), v));
                continue;
            }
            int eq = s.indexOf(eqStr);
            if (eq > 1) {
                String f = s.substring(0, eq);
                String v = s.substring(eq + eqStr.length());
                queue.addComparator(new Comparator(true, retriever.getFieldValue(f), v));
            }
        }

        return queue.compare();
    }

    private static final class Comparator {
        final boolean eqOrNot;
        final String v1;
        final String v2;

        Comparator(boolean eqOrNot, String v1, String v2) {
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
        final List<Comparator> comparators = new ArrayList<>();

        AndOrQueue(boolean andOr) {
            this.andOr = andOr;
        }

        void addComparator(Comparator comparator) {
            comparators.add(comparator);
        }

        boolean compare() {
            if (andOr) {
                for (Comparator c : comparators) {
                    if (!c.compare()) {
                        return false;
                    }
                }
                return true;
            } else {
                for (Comparator c : comparators) {
                    if (c.compare()) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    public interface FieldValueRetriever {
        String getFieldValue(String fieldName) throws Exception;
    }

    private ModelUtil() {
    }
}
