package qingzhou.engine.impl.core.loader;

import qingzhou.engine.impl.EngineContext;
import qingzhou.engine.impl.Main;
import qingzhou.engine.impl.core.ModuleInfo;
import qingzhou.engine.impl.core.ModuleLoaderBuilder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class FilterLoading implements ModuleLoaderBuilder {
    @Override
    public void build(List<ModuleInfo> moduleInfoList, EngineContext engineContext) throws Exception {
        // 系统 api
        URLClassLoader appApiLoader = new URLClassLoader(new URL[]
                {new File(engineContext.getLibDir(), "qingzhou-api.jar").toURI().toURL()},
                Main.class.getClassLoader());

        // 模块 api
        FilterClassLoader moduleApiLoader = new FilterClassLoader(new URL[0], appApiLoader);
        for (ModuleInfo moduleInfo : moduleInfoList) {
            moduleApiLoader.addClassPathFile(moduleInfo.getFile());
            FileFilter fileFilter = new FileFilter();
            fileFilter.includePackages.add(moduleInfo.getName().replace("-", "."));
            moduleApiLoader.addFileFilter(fileFilter);
        }

        // 各个模块
        for (ModuleInfo moduleInfo : moduleInfoList) {
            FilterClassLoader loader = new FilterClassLoader(
                    new URL[]{moduleInfo.getFile().toURI().toURL()},
                    moduleApiLoader);
            FileFilter fileFilter = new FileFilter();
            fileFilter.excludePackages.add(moduleInfo.getName().replace("-", "."));
            loader.addFileFilter(fileFilter);
            moduleInfo.setLoader(loader);
        }
    }

    static class FilterClassLoader extends URLClassLoader {

        final List<FileFilter> fileFilters = new ArrayList<>();

        FilterClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        void addFileFilter(FileFilter fileFilter) {
            fileFilters.add(fileFilter);
        }

        void addClassPathFile(File file) {
            try {
                super.addURL(file.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (excludeClass(name)) {
                throw new ClassNotFoundException(name);
            }

            return super.findClass(name);
        }

        @Override
        public URL findResource(String name) {
            if (excludeResource(name)) {
                return null;
            }

            return super.findResource(name);
        }

        @Override
        public Enumeration<URL> findResources(String name) throws IOException {
            if (excludeResource(name)) {
                return java.util.Collections.emptyEnumeration();
            }

            return super.findResources(name);
        }

        private boolean excludeClass(String name) {
            return fileFilters.stream().noneMatch(fileFilter -> fileFilter.accept(name));
        }

        private boolean excludeResource(String name) {
            if (name.contains("/")) {
                // findResource 也会进入，这个时候 name 为 qingzhou/deployer/CharMap.txt，不是 class，所以去掉后缀以和 class 的判断方式保持一致
                int i = name.lastIndexOf(".");
                if (i != -1) {
                    name = name.substring(0, i);
                }
                name = name.replace("/", ".");
            }

            return excludeClass(name);
        }
    }

    static class FileFilter {
        List<String> includePackages = new ArrayList<>();
        List<String> excludePackages = new ArrayList<>();

        boolean accept(String name) {
            if (!includePackages.isEmpty()) {
                return includePackages.stream().anyMatch(pkg -> matchPkg(name, pkg));
            } else {
                return excludePackages.stream().noneMatch(pkg -> matchPkg(name, pkg));
            }
        }

        boolean matchPkg(String name, String pkg) {
            return name.equals(pkg)
                    ||
                    (name.startsWith(pkg)
                            && !name.substring(pkg.length() + 1).contains("."));
        }
    }
}
