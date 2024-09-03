package qingzhou.deployer.impl;

import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.Request;
import qingzhou.api.type.*;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.ResponseImpl;
import qingzhou.engine.util.FileUtil;
import qingzhou.registry.AppInfo;
import qingzhou.registry.ModelActionInfo;
import qingzhou.registry.ModelFieldInfo;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.*;

class DefaultAction {
    static final List<ModelActionInfo> allDefaultActionCache;

    static {
        allDefaultActionCache = DeployerImpl.parseModelActionInfos(new AnnotationReader(DefaultAction.class));
    }

    private final int DOWNLOAD_BLOCK_SIZE = Integer.parseInt(System.getProperty("qingzhou.DOWNLOAD_BLOCK_SIZE", String.valueOf(1024 * 1024 * 2)));
    private final AppImpl app;
    private final ModelBase instance;

    DefaultAction(AppImpl app, ModelBase instance) {
        this.app = app;
        this.instance = instance;
    }

    private AppInfo getAppInfo() {
        return app.getAppInfo();
    }

    @ModelAction(
            code = DeployerConstants.ACTION_SHOW, icon = "folder-open-alt",
            name = {"查看", "en:Show"},
            info = {"查看该组件的相关信息。", "en:View the information of this model."})
    public void show(Request request) throws Exception {
        Map<String, String> data = ((Showable) instance).showData(request.getId());
        request.getResponse().addData(data);
    }

    @ModelAction(
            code = DeployerConstants.ACTION_LIST, icon = "list",
            name = {"列表", "en:List"},
            info = {"展示该类型的所有组件数据或界面。", "en:Show all component data or interfaces of this type."})
    public void list(Request request) throws Exception {
        ResponseImpl responseImpl = (ResponseImpl) request.getResponse();
        responseImpl.setTotalSize(((Listable) instance).totalSize());
        responseImpl.setPageSize(10);

        int pageNum = 1;
        try {
            pageNum = Integer.parseInt(request.getParameter("pageNum"));
        } catch (NumberFormatException ignored) {
        }
        responseImpl.setPageNum(pageNum);

        String[] fieldNamesToList = getAppInfo().getModelInfo(request.getModel()).getFormFieldList();
        List<Map<String, String>> result = ((Listable) instance).listData(responseImpl.getPageNum(), responseImpl.getPageSize(), fieldNamesToList);
        for (Map<String, String> data : result) {
            request.getResponse().addData(data);
        }
    }

    @ModelAction(
            code = DeployerConstants.ACTION_CREATE, icon = "plus-sign",
            name = {"创建", "en:Create"},
            info = {"获得创建该组件的默认数据或界面。", "en:Get the default data or interface for creating this component."})
    public void create(Request request) throws Exception {
        Map<String, String> properties = getAppInfo().getModelInfo(request.getModel()).getFormFieldDefaultValues();
        request.getResponse().addData(properties);
    }

    @ModelAction(
            code = DeployerConstants.ACTION_ADD, icon = "save",
            ajax = true,
            name = {"添加", "en:Add"},
            info = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    public void add(Request request) throws Exception {
        Map<String, String> properties = prepareParameters(request);
        if (request.getResponse().isSuccess()) {
            ((Addable) instance).addData(properties);
        }
    }

    @ModelAction(
            code = DeployerConstants.ACTION_EDIT, icon = "edit", order = 1,
            name = {"编辑", "en:Edit"},
            info = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
    public void edit(Request request) throws Exception {
        show(request);
    }

    @ModelAction(
            code = DeployerConstants.ACTION_UPDATE, icon = "save",
            ajax = true,
            name = {"更新", "en:Update"},
            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request) throws Exception {
        Map<String, String> properties = prepareParameters(request);
        if (request.getResponse().isSuccess()) {
            ((Updatable) instance).updateData(properties);
        }
    }

    @ModelAction(
            code = DeployerConstants.ACTION_DELETE, icon = "trash", order = 9,
            ajax = true,
            batch = true,
            name = {"删除", "en:Delete"},
            info = {"删除本条数据，注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this data, note: Please operate with caution, it cannot be restored after deletion."})
    public void delete(Request request) throws Exception {
        ((Deletable) instance).deleteData(request.getId());
    }

    private Map<String, String> prepareParameters(Request request) {
        Map<String, String> properties = new HashMap<>();
        for (String fieldName : getAppInfo().getModelInfo(request.getModel()).getFormFieldNames()) {
            String value = request.getParameter(fieldName);
            if (value != null) {
                properties.put(fieldName, value);
            }
        }
        return properties;
    }

    @ModelAction(
            code = DeployerConstants.ACTION_MONITOR, icon = "line-chart", order = 2,
            name = {"监视", "en:Monitor"},
            info = {"获取该组件的运行状态信息，该信息可反映组件的健康情况。",
                    "en:Obtain the operating status information of the component, which can reflect the health of the component."})
    public void monitor(Request request) {
        Map<String, String> p = ((Monitorable) instance).monitorData();

        if (p == null || p.isEmpty()) {
            return;
        }
        Map<String, String> monitorData = new HashMap<>();
        Map<String, String> infoData = new HashMap<>();
        String[] monitorFieldNames = getAppInfo().getModelInfo(request.getModel()).getMonitorFieldNames();
        for (String fieldName : monitorFieldNames) {
            ModelFieldInfo monitorField = getAppInfo().getModelInfo(request.getModel()).getModelFieldInfo(fieldName);
            String value = p.get(fieldName);
            if (value == null) continue;

            if (monitorField.isNumeric()) {
                monitorData.put(fieldName, value);
            }
        }

        String[] infoFieldNames = getAppInfo().getModelInfo(request.getModel()).getFormFieldNames();
        for (String infoFieldName : infoFieldNames) {
            String value = p.get(infoFieldName);
            if (value == null) continue;

            infoData.put(infoFieldName, value);
        }

        request.getResponse().addData(monitorData);
        request.getResponse().addData(infoData);
    }

    @ModelAction(
            code = DeployerConstants.ACTION_FILES, icon = "download-alt",
            ajax = true,
            order = 8,
            name = {"下载", "en:Download"},
            info = {"获取该组件可下载文件的列表。",
                    "en:Gets a list of downloadable files for this component."})
    public void files(Request request) throws Exception {
        String id = request.getId();
        if (id.contains("..")) {
            throw new IllegalArgumentException();
        }
        File fileBase = ((Downloadable) instance).downloadData(id);
        if (!fileBase.isDirectory()) return;
        File[] files = fileBase.listFiles();
        if (files == null) return;

        HashMap<String, String> map = new HashMap<>();
        for (File rootFile : files) {
            String downloadItem = rootFile.getName();
            if (rootFile.isDirectory()) {
                File[] subFiles = rootFile.listFiles();
                if (subFiles != null) {
                    for (File subFile : subFiles) {
                        map.put(downloadItem + "/" + subFile.getName(), subFile.getName() + " (" + FileUtil.getFileSize(subFile) + ")");
                    }
                }
            } else if (rootFile.isFile()) {
                map.put(downloadItem, downloadItem + " (" + FileUtil.getFileSize(rootFile) + ")");
            }
        }
        request.getResponse().addData(map);
    }

    @ModelAction(
            code = DeployerConstants.ACTION_DOWNLOAD, icon = "download-alt",
            name = {"下载文件", "en:Download File"},
            info = {"下载指定的文件集合，这些文件须在该组件的可下载文件列表内。",
                    "en:Downloads the specified set of files that are in the component list of downloadable files."})
    public void download(Request request) throws Exception {
        File keyDir = new File(app.getAppContext().getTemp(), "download");

        String downloadKey = request.getParameter("DOWNLOAD_KEY");
        if (downloadKey == null || downloadKey.trim().isEmpty()) {
            String downloadFileNames = request.getParameter("downloadFileNames");

            // check
            if (downloadFileNames == null || downloadFileNames.trim().isEmpty()) {
                request.getResponse().setMsg("No file name found.");
                request.getResponse().setSuccess(false);
                return;
            }
            if (downloadFileNames.contains("..")) throw new IllegalArgumentException();

            File fileBase = ((Downloadable) instance).downloadData(request.getId());
            List<File> downloadFiles = new ArrayList<>();
            for (String s : downloadFileNames.split(",")) {
                downloadFiles.add(FileUtil.newFile(fileBase, s));//防止路径穿越：FileUtil.newFile
            }
            downloadKey = buildDownloadKey(downloadFiles, keyDir);
        }

        long downloadOffset = 0;
        String downloadOffsetParameter = request.getParameter("DOWNLOAD_OFFSET");
        if (downloadOffsetParameter != null && !downloadOffsetParameter.trim().isEmpty()) {
            downloadOffset = Long.parseLong(downloadOffsetParameter.trim());
        }

        Map<String, String> downloadedStatus = downloadFile(downloadKey, downloadOffset, keyDir);
        if (downloadedStatus != null) {
            request.getResponse().addData(downloadedStatus);
        }
    }

    // 为支持大文件续传，下载必需有 key
    private Map<String, String> downloadFile(String key, long offset, File baseDir) throws Exception {
        if (key == null || key.trim().isEmpty() || !new File(baseDir, key).exists()) return null;
        if (offset < 0) return null;

        Map<String, String> result = new HashMap<>();
        File downloadFile = new File(baseDir, key);
        result.put("DOWNLOAD_KEY", key);

        byte[] byteRead;
        boolean hasMore = false;
        try (RandomAccessFile raf = new RandomAccessFile(downloadFile, "r")) {
            raf.seek(offset);
            byte[] block = new byte[DOWNLOAD_BLOCK_SIZE];
            int read = raf.read(block);
            if (read > 0) { // ==0 表示上次正好读取到结尾
                if (read == block.length) {
                    byteRead = block;

                    if (raf.getFilePointer() < raf.length() - 1) {
                        hasMore = true;
                    }
                } else {
                    byteRead = new byte[read];
                    System.arraycopy(block, 0, byteRead, 0, read);
                }

                result.put("DOWNLOAD_BLOCK", Controller.cryptoService.getBase64Coder().encode(byteRead));
                offset = raf.getFilePointer();
            }
        }

        if (hasMore) {
            result.put("DOWNLOAD_OFFSET", String.valueOf(offset));
        } else {
            result.put("DOWNLOAD_OFFSET", String.valueOf(-1L));
            File temp = FileUtil.newFile(baseDir, key);
            FileUtil.forceDelete(temp);
        }

        return result;
    }

    // 为支持大文件续传，下载必需有 key
    private String buildDownloadKey(List<File> downloadFiles, File keyDir) throws IOException {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        String keySP = "-";

        // 清理历史的临时文件
        String[] list = keyDir.list();
        if (list != null) {
            for (String historicalKey : list) {
                boolean delete = false;
                try {
                    int i = historicalKey.indexOf(keySP);
                    long time = format.parse(historicalKey.substring(0, i)).getTime();
                    if (System.currentTimeMillis() - time > 1000 * 60 * 60 * 24 * 7) { // 清理可能下载失败的文件
                        delete = true;
                    }
                } catch (Exception e) {
                    System.err.println("failed to parse time: " + historicalKey);
                }
                if (delete) {
                    File temp = FileUtil.newFile(keyDir, historicalKey);
                    FileUtil.forceDelete(temp);
                }
            }
        }

        String key = format.format(new Date()) + keySP + UUID.randomUUID().toString().replace("-", "");
        File zipTo = FileUtil.newFile(keyDir, key);
        File tempDir = FileUtil.newFile(keyDir, UUID.randomUUID().toString());// 保障压缩文件的层次结构
        try {
            for (File file : downloadFiles) {
                if (file.exists()) {
                    File copyTo = new File(tempDir, file.getName());
                    if (file.isDirectory()) {
                        FileUtil.mkdirs(copyTo);
                    }
                    FileUtil.copyFileOrDirectory(file, copyTo);
                }
            }
            FileUtil.zipFiles(tempDir, zipTo, false);
        } finally {
            FileUtil.forceDelete(tempDir);
        }
        return key;
    }
}
