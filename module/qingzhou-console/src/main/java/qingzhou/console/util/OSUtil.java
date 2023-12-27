package qingzhou.console.util;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;

public class OSUtil {
    private static final String OS_NAME_PROPERTY = "os.name";

    public static final boolean IS_MAC_OS;
    public static final boolean IS_WINDOWS;
    public static final boolean IS_LINUX;

    static {
        // This check is derived from the check in Commons Lang
        String osName;
        if (System.getSecurityManager() == null) {
            osName = System.getProperty(OS_NAME_PROPERTY);
        } else {
            osName = AccessController.doPrivileged(
                    (PrivilegedAction<String>) () -> System.getProperty(OS_NAME_PROPERTY));
        }

        IS_MAC_OS = osName.toLowerCase(Locale.ENGLISH).startsWith("mac os x");

        IS_WINDOWS = osName.startsWith("Windows");

        IS_LINUX = !IS_MAC_OS && !IS_WINDOWS;
    }

}
