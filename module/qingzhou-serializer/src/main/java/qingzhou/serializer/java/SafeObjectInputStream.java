package qingzhou.serializer.java;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;
import java.util.HashMap;

public class SafeObjectInputStream extends ObjectInputStream {

    /**
     * table mapping primitive type names to corresponding class objects
     */
    private static final HashMap<String, Class<?>> primClasses
            = new HashMap<>(8, 1.0F);

    static {
        primClasses.put("boolean", boolean.class);
        primClasses.put("byte", byte.class);
        primClasses.put("char", char.class);
        primClasses.put("short", short.class);
        primClasses.put("int", int.class);
        primClasses.put("long", long.class);
        primClasses.put("float", float.class);
        primClasses.put("double", double.class);
        primClasses.put("void", void.class);
    }

    public SafeObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass classDesc) throws ClassNotFoundException {
        final String name = BlacklistClassResolver.DEFAULT.check(classDesc.getName());
        try {
            return Class.forName(name, false, getClassloader());
        } catch (ClassNotFoundException e) {
            // 来自 super 的方法
            Class<?> cl = primClasses.get(name);
            if (cl != null) {
                return cl;
            } else {
                throw e;
            }
        }
    }

    @Override
    protected Class<?> resolveProxyClass(final String[] interfaces) throws ClassNotFoundException {
        final Class<?>[] cinterfaces = new Class[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            cinterfaces[i] = Class.forName(interfaces[i], false, getClassloader());
        }

        try {
            return Proxy.getProxyClass(getClassloader(), cinterfaces);
        } catch (final IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
        }
    }

    ClassLoader getClassloader() {
        return Thread.currentThread().getContextClassLoader();
    }

}
