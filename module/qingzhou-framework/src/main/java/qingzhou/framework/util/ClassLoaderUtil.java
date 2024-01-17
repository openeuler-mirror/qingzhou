package qingzhou.framework.util;

import qingzhou.framework.pattern.Callback;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class ClassLoaderUtil {
    public static URLClassLoader newURLClassLoader(List<File> files, ClassLoader parentLoader) {
        List<URL> urls = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                try {
                    urls.add(file.toURI().toURL());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]), parentLoader);
    }

    public static <T> void runUnderClassLoader(Callback<Void, T> run, ClassLoader loader) throws Exception {
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(loader);
            run.run(null);
        } finally {
            Thread.currentThread().setContextClassLoader(oldLoader);
        }
    }

    private ClassLoaderUtil() {
    }
}
