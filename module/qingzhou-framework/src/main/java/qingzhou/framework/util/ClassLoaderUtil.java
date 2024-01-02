package qingzhou.framework.util;

import qingzhou.framework.pattern.Callback;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

public class ClassLoaderUtil {
    public static <S> List<S> loadServices(String serviceTypeClass, ClassLoader... loaderOrNot) {
        List<S> services = new ArrayList<>();

        try {
            ClassLoader loader = null;
            if (loaderOrNot != null) {
                for (ClassLoader classLoader : loaderOrNot) {
                    if (classLoader != null) {
                        loader = classLoader;
                        break;
                    }
                }
            }
            if (loader == null) {
                loader = Thread.currentThread().getContextClassLoader();
            }
            if (loader == null) {
                loader = ClassLoader.getSystemClassLoader();
            }
            Objects.requireNonNull(loader);

            Enumeration<URL> resources = loader.getResources("META-INF/services/" + serviceTypeClass);
            while (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                URLConnection urlConnection = url.openConnection();
                urlConnection.setDefaultUseCaches(false);
                urlConnection.setUseCaches(false);
                try (BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()), 256)) {
                    ClassLoader finalLoader = loader;
                    br.lines().filter(s -> s.charAt(0) != '#').map(String::trim).forEach(line -> {
                        try {
                            services.add((S) finalLoader.loadClass(line).newInstance());
                        } catch (Exception e) {
                            e.printStackTrace();// 不需要抛异常，加了条目没有类打个日志即可
                        }
                    });
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return services;
    }

    public static URLClassLoader newURLClassLoader(List<File> files, ClassLoader parentLoader) {
        return newURLClassLoader(files.toArray(new File[0]), parentLoader);
    }

    public static URLClassLoader newURLClassLoader(File file, ClassLoader parentLoader) {
        return newURLClassLoader(new File[]{file}, parentLoader);
    }

    public static URLClassLoader newURLClassLoader(File[] files, ClassLoader parentLoader) {
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

    public static Class<?> loadClass(String className, Callback<Void, ClassLoader> secondLoader) {
        Class<?> objClass = null;
        try {
            objClass = Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (Exception e) {
            try {
                ClassLoader loader = secondLoader.run(null);
                objClass = loader.loadClass(className);
            } catch (Exception ignored) {
            }
        }
        return objClass;
    }

    public static <T> T runUnderClassLoader(Callback<Void, T> run, ClassLoader loader) throws Exception {
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(loader);
            return run.run(null);
        } finally {
            Thread.currentThread().setContextClassLoader(oldLoader);
        }
    }

    private ClassLoaderUtil() {
    }
}
