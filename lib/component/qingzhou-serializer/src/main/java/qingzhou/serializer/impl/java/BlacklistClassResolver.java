package qingzhou.serializer.impl.java;

import java.util.regex.Pattern;

public class BlacklistClassResolver {
    public static final BlacklistClassResolver DEFAULT = new BlacklistClassResolver();


    public static final Pattern PRIMITIVE_ARRAY = Pattern.compile("^\\[+[BCDFIJSVZ]$");

    private final String[] blacklist;
    private final String[] whitelist;

    protected BlacklistClassResolver() {
        String whitelist = "org.codehaus.groovy.runtime.,org.apache.commons.collections.functors.,org.apache.commons.collections4.functors.,org.apache.xalan,java.lang.Process,javax.management.BadAttributeValueExpException,com.sun.org.apache.xalan,org.springframework.beans.factory.ObjectFactory,org.apache.commons.fileupload,org.apache.commons.beanutils,java.util.,java.lang.,qingzhou.";// 安全起见，不要修改，应该追加
        String blacklist = "";// 安全起见，默认没有白名单，不允许序列化任何类
        this.whitelist = toArray(whitelist);
        this.blacklist = toArray(blacklist);
    }

    private static String[] toArray(String property) {
        return property == null || property.trim().isEmpty() ? null : property.trim().split(" *, *");
    }

    private static boolean contains(final String[] list, final String name) {
        if (list != null) {
            for (final String white : list) {
                if ("*".equals(white) || name.startsWith(white)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean isBlacklisted(final String name) {

        // allow primitive arrays
        if (PRIMITIVE_ARRAY.matcher(name).matches()) {
            return false;
        }

        if (name.startsWith("[L") && name.endsWith(";")) {
            return isBlacklisted(name.substring(2, name.length() - 1));
        }
        return (whitelist != null && !contains(whitelist, name)) || contains(blacklist, name);
    }

    public final String check(final String name) {
        if (isBlacklisted(name)) {
            throw new SecurityException(name + " is not whitelisted as deserialisable, prevented before loading it");
        }
        return name;
    }
}
