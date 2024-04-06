package qingzhou.engine.impl;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * ClassLoader that is composed of other classloaders. Each loader will be used to try to load the particular class,
 * until one of them succeeds. <b>Note:</b> The loaders will always be called in the REVERSE order they were added in.
 * <p>
 * The Composite class loader also has registered the classloader that loaded xstream.jar and (if available) the
 * thread's context classloader.
 * </p >
 * <h1>Example</h1>
 *
 * <pre>
 * <code>CompositeClassLoader loader = new CompositeClassLoader();
 * loader.add(MyClass.class.getClassLoader());
 * loader.add(new AnotherClassLoader());
 * &nbsp;
 * loader.loadClass("com.blah.ChickenPlucker");
 * </code>
 * </pre>
 * <p>
 * The above code will attempt to load a class from the following classloaders (in order):
 * </p >
 * <ul>
 * <li>AnotherClassLoader (and all its parents)</li>
 * <li>The classloader for MyClas (and all its parents)</li>
 * <li>The thread's context classloader (and all its parents)</li>
 * <li>The classloader for XStream (and all its parents)</li>
 * </ul>
 * <p>
 * The added classloaders are kept with weak references to allow an application container to reload classes.
 * </p >
 *
 * @author Joe Walnes
 * @author J&ouml;rg Schaible
 * @since 1.0.3
 */
public class CompositeClassLoader extends URLClassLoader {
    static {
        // see http://www.cs.duke.edu/csed/java/jdk1.7/technotes/guides/lang/cl-mt.html
        registerAsParallelCapable();
    }

    private final ReferenceQueue<ClassLoader> queue = new ReferenceQueue<>();
    private final List<WeakReference<ClassLoader>> classLoaders = new ArrayList<>();

    public CompositeClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    /**
     * Add a loader to the n
     */
    public synchronized void add(final ClassLoader classLoader) {
        cleanup();
        if (classLoader != null) {
            addInternal(classLoader);
        }
    }

    private void addInternal(final ClassLoader classLoader) {
        WeakReference<ClassLoader> refClassLoader = null;
        for (final Iterator<WeakReference<ClassLoader>> iterator = classLoaders.iterator(); iterator.hasNext(); ) {
            final WeakReference<ClassLoader> ref = iterator.next();
            final ClassLoader cl = ref.get();
            if (cl == null) {
                iterator.remove();
            } else if (cl == classLoader) {
                iterator.remove();
                refClassLoader = ref;
            }
        }
        classLoaders.add(0, refClassLoader != null ? refClassLoader : new WeakReference<>(classLoader, queue));
    }

    private List<ClassLoader> getClassLoaders() {
        final List<ClassLoader> copy = new ArrayList<>(classLoaders.size());
        synchronized (this) {
            cleanup();
            for (final WeakReference<ClassLoader> ref : classLoaders) {
                final ClassLoader cl = ref.get();
                if (cl != null) {
                    copy.add(cl);
                }
            }
        }
        return copy;
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        final List<ClassLoader> copy = getClassLoaders();

        for (final ClassLoader classLoader : copy) {
            try {
                return classLoader.loadClass(name);
            } catch (final ClassNotFoundException notFound) {
                // ok.. try another one
            }
        }

        throw new ClassNotFoundException(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<ClassLoader> copy = getClassLoaders();
        Set<URL> urlSet = new HashSet<>();
        for (ClassLoader classLoader : copy) {
            Enumeration<URL> resources = classLoader.getResources(name);
            while (resources.hasMoreElements()) {
                urlSet.add(resources.nextElement());
            }
        }

        return Collections.enumeration(urlSet);
    }

    private void cleanup() {
        Reference<? extends ClassLoader> ref;
        while ((ref = queue.poll()) != null) {
            classLoaders.remove(ref);
        }
    }

    @Override
    public URL[] getURLs() { // for com.tongweb.server.loader.WebappLoader.buildClassPath
        List<URL> result = new ArrayList<>();
        for (WeakReference<ClassLoader> classLoader : classLoaders) {
            ClassLoader loader = classLoader.get();
            if (loader instanceof URLClassLoader) {
                result.addAll(Arrays.asList(((URLClassLoader) loader).getURLs()));
            }
        }

        return result.toArray(new URL[0]);
    }

    @Override
    public void close() throws IOException {
        super.close();
        for (WeakReference<ClassLoader> classLoader : classLoaders) {
            ClassLoader loader = classLoader.get();
            if (loader instanceof Closeable) {
                ((Closeable) loader).close();
            }
        }
    }
}