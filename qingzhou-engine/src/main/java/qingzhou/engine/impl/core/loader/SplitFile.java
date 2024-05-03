package qingzhou.engine.impl.core.loader;

import qingzhou.engine.impl.EngineContext;
import qingzhou.engine.impl.Main;
import qingzhou.engine.impl.core.ModuleInfo;
import qingzhou.engine.impl.core.ModuleLoaderBuilder;
import qingzhou.engine.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class SplitFile implements ModuleLoaderBuilder {
    @Override
    public void build(List<ModuleInfo> moduleInfoList, EngineContext engineContext) throws Exception {
        String moduleApiFileName = "module-api.jar";
        String moduleImplSuffixName = "-impl.jar";
        File libCache = Utils.newFile(engineContext.getTemp(), "engine-lib");
        splitFiles(moduleInfoList, libCache, moduleApiFileName, moduleImplSuffixName);

        URLClassLoader appApiLoader = new URLClassLoader(new URL[]
                {new File(engineContext.getLibDir(), "qingzhou-api.jar").toURI().toURL()},
                Main.class.getClassLoader());

        URLClassLoader moduleApiLoader = new URLClassLoader(new URL[]{new File(libCache, moduleApiFileName).toURI().toURL()}, appApiLoader);

        for (ModuleInfo moduleInfo : moduleInfoList) {
            File file = new File(libCache, moduleInfo.getName() + moduleImplSuffixName);
            try {
                URLClassLoader implLoader = new URLClassLoader(new URL[]{file.toURI().toURL()}, moduleApiLoader);
                moduleInfo.setLoader(implLoader);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private void splitFiles(List<ModuleInfo> moduleInfoList, File tempBase, String apiFileName, String implSuffix) throws Exception {
        Utils.mkdirs(tempBase);
        Utils.cleanDirectory(tempBase);
        // 合并 api jar
        Set<String> alreadyNames = new HashSet<>();
        try (ZipOutputFile apiJar = new ZipOutputFile(new File(tempBase, apiFileName))) {
            for (ModuleInfo moduleInfo : moduleInfoList) {
                String moduleName = moduleInfo.getName();
                File moduleFile = moduleInfo.getFile();
                try (ZipInputStream moduleZip = new ZipInputStream(Files.newInputStream(moduleFile.toPath()))) {
                    File moduleImplFile = new File(tempBase, moduleName + implSuffix);
                    try (ZipOutputFile moduleImplJar = new ZipOutputFile(moduleImplFile)) {
                        String apiPkg = moduleName.replace("-", "/") + "/";
                        for (ZipEntry zipEntry; (zipEntry = moduleZip.getNextEntry()) != null; ) {
                            try {
                                String entryName = zipEntry.getName();

                                if (zipEntry.isDirectory()) {
                                    moduleImplJar.writeZipEntry(entryName, true, null);
                                    if (apiPkg.startsWith(entryName)) {
                                        if (alreadyNames.add(entryName)) { // 多个 jar 都有 qingzhou/ 这个根目录，多次会重复报错
                                            apiJar.writeZipEntry(entryName, true, null);
                                        }
                                    }
                                } else {
                                    boolean isApiResource = false;
                                    if (entryName.startsWith(apiPkg)) {
                                        int inner = entryName.indexOf("/", apiPkg.length());
                                        if (inner == -1) { // 不需要子目录
                                            // 写入 api jar
                                            apiJar.writeZipEntry(entryName, false, moduleZip);
                                            isApiResource = true;
                                        }
                                    }
                                    // 其它文件，都切割到 impl jar
                                    if (!isApiResource) {
                                        moduleImplJar.writeZipEntry(entryName, false, moduleZip);
                                    }
                                }
                            } finally {
                                moduleZip.closeEntry();
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
                Utils.copyStream(entryStream, zos);
            }

            this.zos.closeEntry();
        }

        @Override
        public void close() throws Exception {
            this.zos.close();
        }
    }
}
