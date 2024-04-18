package qingzhou.engine.impl;

import qingzhou.engine.Module;
import qingzhou.engine.ServiceRegister;
import qingzhou.engine.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Main {
    private final List<Module> moduleList = new ArrayList<>();
    private final ModuleContextImpl moduleContext = new ModuleContextImpl();

    public static void main(String[] args) throws Exception {
        new Main().start();
    }

    private void start() throws Exception {
        LinkedHashMap<File, Set<File>> startLevelFiles = loadStartLevelFiles();

        ClassLoader[] moduleLoaders = buildModuleLoaders(startLevelFiles);

        for (ClassLoader moduleLoader : moduleLoaders) {
            startModule(moduleLoader);
        }

        waitForStop();
    }

    private ClassLoader[] buildModuleLoaders(LinkedHashMap<File, Set<File>> startLevel) throws Exception {
        File tempBase = moduleContext.getTemp();
        String moduleApiFileName = "module-api.jar";
        List<String> implFileNames = new ArrayList<>();
        ArrayList<File> files = new ArrayList<>(startLevel.keySet());
        Collections.reverse(files);
        splitApiFiles(files, tempBase, moduleApiFileName, implFileNames);

        File apiJarFile = new File(moduleContext.getLibDir(), "qingzhou-api.jar");
        URLClassLoader apiLoader = new URLClassLoader(new URL[]{apiJarFile.toURI().toURL()}, Main.class.getClassLoader());

        URLClassLoader moduleApiLoader = new URLClassLoader(new URL[]{new File(tempBase, moduleApiFileName).toURI().toURL()}, apiLoader);

        List<ClassLoader> loaderList = new ArrayList<>();
        for (String implFileName : implFileNames) {
            loaderList.add(new URLClassLoader(new URL[]{new File(tempBase, implFileName).toURI().toURL()}, moduleApiLoader));
        }
        return loaderList.toArray(new ClassLoader[0]);
    }

    private void splitApiFiles(List<File> moduleLevels, File loaderTemp, String apiFileName, List<String> implFileNameContainer) throws Exception {
        // 合并 api jar
        try (ZipOutputFile apiJar = new ZipOutputFile(new File(loaderTemp, apiFileName))) {
            Set<String> alreadyNames = new HashSet<>();
            for (File moduleFile : moduleLevels) {
                try (ZipFile moduleZip = new ZipFile(moduleFile)) {
                    String moduleFileName = moduleFile.getName();
                    String implFileName = moduleFileName.substring(0, moduleFileName.length() - 4) + "-impl.jar";
                    implFileNameContainer.add(implFileName);
                    try (ZipOutputFile moduleImplJar = new ZipOutputFile(new File(loaderTemp, implFileName))) {
                        Enumeration<? extends ZipEntry> entries = moduleZip.entries();

                        String moduleName = moduleFileName.substring(0, moduleFileName.length() - ".jar".length());
                        String prefix = moduleName.replace("-", "/") + "/";
                        while (entries.hasMoreElements()) {
                            ZipEntry zipEntry = entries.nextElement();
                            String entryName = zipEntry.getName();

                            // 写入 api jar
                            if (zipEntry.isDirectory() && prefix.startsWith(entryName)) {
                                if (alreadyNames.add(entryName)) { // 多个 jar 都有 qingzhou 这个父目录，多次会重复报错
                                    apiJar.writeZipEntry(entryName, true, null);
                                }
                                continue;
                            }
                            if (!zipEntry.isDirectory() && entryName.startsWith(prefix)) {
                                int inner = entryName.indexOf("/", prefix.length());
                                if (inner == -1) { // 不需要子目录
                                    apiJar.writeZipEntry(entryName, false,
                                            moduleZip.getInputStream(zipEntry));
                                    continue;
                                }
                            }

                            // 其它文件，都切割到 impl jar
                            if (zipEntry.isDirectory()) {
                                moduleImplJar.writeZipEntry(entryName, true, null);
                            } else {
                                moduleImplJar.writeZipEntry(entryName, false,
                                        moduleZip.getInputStream(zipEntry));
                            }
                        }
                    }
                }
            }
        }
    }

    private static class ZipOutputFile implements AutoCloseable {
        final ZipOutputStream zos;

        private ZipOutputFile(File file) throws IOException {
            this.zos = new ZipOutputStream(Files.newOutputStream(file.toPath()));
        }

        void writeZipEntry(String entryName, boolean isDirectory, InputStream entryStream) throws IOException {
            this.zos.putNextEntry(new ZipEntry(entryName));

            if (!isDirectory) {
                FileUtil.copyStream(entryStream, zos);
            }

            this.zos.closeEntry();
        }

        @Override
        public void close() throws Exception {
            this.zos.close();
        }
    }

    private LinkedHashMap<File, Set<File>> loadStartLevelFiles() throws Exception {
        LinkedHashMap<File, Set<File>> startLevel = new LinkedHashMap<>();

        String moduleConfigFile = "/module.properties";
        try (InputStream inputStream = Main.class.getResourceAsStream(moduleConfigFile)) {
            if (inputStream == null) throw new IllegalStateException("Not found: " + moduleConfigFile);

            LinkedHashMap<String, String> data = FileUtil.streamToProperties(inputStream);
            File moduleDir = new File(moduleContext.getLibDir(), "module");
            data.forEach((key, vs) -> {
                File kJar = new File(moduleDir, key + ".jar");
                if (kJar.isFile()) {
                    Set<File> vJars = new HashSet<>();
                    for (String v : vs.split(",")) {
                        File vJar = new File(moduleDir, v + ".jar");
                        if (vJar.isFile()) {
                            vJars.add(vJar);
                        }
                    }
                    startLevel.put(kJar, vJars);
                }
            });
        }

        return startLevel;
    }

    private void startModule(ClassLoader classLoader) {
        try {
            for (Module module : findModules(classLoader)) {
                if (module instanceof ServiceRegister) {
                    Class<?> serviceType = ((ServiceRegister<?>) module).serviceType();
                    if (moduleList.stream().anyMatch(module1 -> (module1 instanceof ServiceRegister) && (((ServiceRegister<?>) module1).serviceType() == serviceType))) {
                        throw new IllegalStateException("duplicate binding service: " + serviceType);
                    }
                }

                module.start(moduleContext);
                moduleList.add(module);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Module> findModules(ClassLoader classLoader) {
        List<Module> modules = new ArrayList<>();
        ServiceLoader<Module> serviceLoader = ServiceLoader.load(Module.class, classLoader);
        for (Module module : serviceLoader) {
            modules.add(module);
        }
        return modules;
    }

    private void waitForStop() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (int i = moduleList.size() - 1; i >= 0; i--) {
                Module module = moduleList.remove(i);
                module.stop();

                System.out.println("The module has been unloaded: " + module.getClass().getName());
            }
            moduleContext.close();
            System.out.println("Qingzhou has been stopped.");
        }));

        synchronized (Main.class) {
            Main.class.wait();
        }
    }
}
