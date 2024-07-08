package qingzhou.deployer.impl;

import qingzhou.api.*;
import qingzhou.api.DataStore;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.Request;
import qingzhou.api.Response;
import qingzhou.api.type.Downloadable;
import qingzhou.api.type.Listable;
import qingzhou.api.type.Monitorable;
import qingzhou.engine.util.Utils;
import qingzhou.engine.util.crypto.CryptoServiceFactory;
import qingzhou.registry.AppInfo;
import qingzhou.registry.ModelFieldInfo;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class PresetAction {
    private final AppImpl app;
    private final ModelBase instance;

    PresetAction(AppImpl app, ModelBase instance) {
        this.app = app;
        this.instance = instance;
    }

    private AppInfo getAppInfo() {
        return app.getAppInfo();
    }

    @ModelAction(
            name = {"查看", "en:Show"},
            info = {"查看该组件的相关信息。", "en:View the information of this model."})
    public void show(Request request, Response response) throws Exception {
        Map<String, String> data = instance.getDataStore().getDataById(request.getId());
        response.addData(data);
    }

    @ModelAction(
            name = {"监视", "en:Monitor"},
            info = {"获取该组件的运行状态信息，该信息可反映组件的健康情况。",
                    "en:Obtain the operating status information of the component, which can reflect the health of the component."})
    public void monitor(Request request, Response response) {
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

        response.addData(monitorData);
        response.addData(infoData);
    }

    @ModelAction(
            name = {"列表", "en:List"},
            info = {"展示该类型的所有组件数据或界面。", "en:Show all component data or interfaces of this type."})
    public void list(Request request, Response response) throws Exception {
        String modelName = request.getModel();
        DataStore dataStore = instance.getDataStore();
        if (dataStore == null) {
            return;
        }
        int totalSize = dataStore.getTotalSize();
        response.setTotalSize(totalSize);

        response.setPageSize(10);

        int pageNum = 1;
        try {
            pageNum = Integer.parseInt(request.getParameter(Listable.PARAMETER_PAGE_NUM));
        } catch (NumberFormatException ignored) {
        }
        response.setPageNum(pageNum);

        String[] dataIdInPage = dataStore.getDataIdInPage(response.getPageSize(), pageNum).toArray(new String[0]);
        String[] fieldNamesToList = getAppInfo().getModelInfo(modelName).getFormFieldList();
        List<Map<String, String>> result = dataStore.getDataFieldByIds(dataIdInPage, fieldNamesToList);
        for (Map<String, String> data : result) {
            response.addData(data);
        }
    }

    @ModelAction(
            name = {"编辑", "en:Edit"},
            info = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
    public void edit(Request request, Response response) throws Exception {
        show(request, response);
    }

    @ModelAction(
            ajax = true,
            name = {"更新", "en:Update"},
            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request, Response response) throws Exception {
        DataStore dataStore = instance.getDataStore();
        Map<String, String> properties = prepareParameters(request);
        dataStore.updateDataById(request.getId(), properties);
    }

    Map<String, String> prepareParameters(Request request) {
        Map<String, String> properties = new HashMap<>();
        for (String fieldName : getAppInfo().getModelInfo(request.getModel()).getFormFieldNames()) {
            String value = request.getParameter(fieldName);
            if (value != null) {
                properties.put(fieldName, value);
            }
        }
        return properties;
    }

    //// todo：实现移动到内部?
    @ModelAction(
            ajax = true,
            order = 98,
            icon = "download-alt",
            name = {"下载", "en:Download"},
            info = {"获取该组件可下载文件的列表。",
                    "en:Gets a list of downloadable files for this component."})
    public void files(Request request, Response response) throws Exception {
        Map<String, List<String[]>> result = ((Downloadable) instance).downloadlist(request, response);
        if (result != null && !result.isEmpty()) {
            HashMap<String, String> map = new HashMap<>();
            if (result.size() == 1) {
                for (String[] file : result.values().iterator().next()) {
                    map.put(file[0], file[0] + " (" + file[1] + ")");
                }
            } else {
                for (Map.Entry<String, List<String[]>> entry : result.entrySet()) {
                    String group = entry.getKey();
                    for (String[] file : entry.getValue()) {
                        map.put(group + Downloadable.DOWNLOAD_FILE_GROUP_NAME_SEPARATOR + file[0], file[0] + " (" + file[1] + ")");
                    }
                }
            }
            response.addData(map);
        }
    }

    @ModelAction(icon = "download-alt",
            name = {"下载文件", "en:Download File"},
            info = {"下载指定的文件集合，这些文件须在该组件的可下载文件列表内。注：指定的文件集合须以 " + Downloadable.PARAMETER_DOWNLOAD_FILE_NAMES + " 参数传递给服务器，多个文件之间用英文逗号分隔。",
                    "en:Downloads the specified set of files that are in the component list of downloadable files. Note: The specified file set must be passed to the server with the " + Downloadable.PARAMETER_DOWNLOAD_FILE_NAMES + " parameter, and multiple files are separated by commas."})
    public void download(Request request, Response response) throws Exception {
        File keyDir = new File(app.getAppContext().getTemp(), "download");

        String downloadKey = request.getParameter(Downloadable.DOWNLOAD_KEY);
        if (downloadKey == null || downloadKey.trim().isEmpty()) {
            String downloadFileNames = request.getParameter(Downloadable.PARAMETER_DOWNLOAD_FILE_NAMES);

            // check
            if (downloadFileNames == null || downloadFileNames.trim().isEmpty()) {
                response.setMsg("No file name found.");
                response.setSuccess(false);
                return;
            }

            File[] downloadFiles = ((Downloadable) instance).downloadfile(request, downloadFileNames.split(Downloadable.DOWNLOAD_FILE_GROUP_NAME_SEPARATOR));
            downloadKey = buildDownloadKey(downloadFiles, keyDir);
        }

        long downloadOffset = 0;
        String downloadOffsetParameter = request.getParameter(Downloadable.DOWNLOAD_OFFSET);
        if (downloadOffsetParameter != null && !downloadOffsetParameter.trim().isEmpty()) {
            downloadOffset = Long.parseLong(downloadOffsetParameter.trim());
        }

        Map<String, String> downloadedStatus = downloadFile(downloadKey, downloadOffset, keyDir);
        if (downloadedStatus != null) {
            response.addData(downloadedStatus);
        }
    }

    // 为支持大文件续传，下载必需有 key
    Map<String, String> downloadFile(String key, long offset, File keyDir) throws Exception {
        if (key == null || key.trim().isEmpty() || !new File(keyDir, key).exists()) {
            return null;
        }
        if (offset < 0) {
            return null;
        }
        Map<String, String> result = new HashMap<>();

        File downloadFile = new File(keyDir, key);
        result.put(Downloadable.DOWNLOAD_KEY, key);

        byte[] byteRead;
        boolean hasMore = false;
        try (RandomAccessFile raf = new RandomAccessFile(downloadFile, "r")) {
            raf.seek(offset);
            int DOWNLOAD_BLOCK_SIZE = Integer.parseInt(System.getProperty("qingzhou.DOWNLOAD_BLOCK_SIZE", String.valueOf(1024 * 1024 * 2)));
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

                result.put(Downloadable.DOWNLOAD_BLOCK, CryptoServiceFactory.getInstance().getMessageDigest().bytesToHex(byteRead));
                offset = raf.getFilePointer();
            }
        }

        if (hasMore) {
            result.put(Downloadable.DOWNLOAD_OFFSET, String.valueOf(offset));
        } else {
            result.put(Downloadable.DOWNLOAD_OFFSET, String.valueOf(-1L));
            File temp = Utils.newFile(keyDir, key);
            Utils.forceDelete(temp);
        }

        return result;
    }

    // 为支持大文件续传，下载必需有 key
    static String buildDownloadKey(File[] downloadFiles, File keyDir) throws IOException {
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
                    File temp = Utils.newFile(keyDir, historicalKey);
                    Utils.forceDelete(temp);
                }
            }
        }

        String key = format.format(new Date()) + keySP + UUID.randomUUID().toString().replace("-", "");
        File zipTo = Utils.newFile(keyDir, key);
        File tempDir = Utils.newFile(keyDir, UUID.randomUUID().toString());// 保障压缩文件的层次结构
        for (File file : downloadFiles) {
            if (file.exists()) {
                File copyTo = new File(tempDir, file.getName());
                if (file.isDirectory()) {
                    Utils.mkdirs(copyTo);
                }
                Utils.copyFileOrDirectory(file, copyTo);
            }
        }
        Utils.zipFiles(tempDir, zipTo, false);
        Utils.forceDelete(tempDir);
        return key;
    }

    @ModelAction(batch = true, ajax = true,
            name = {"删除", "en:Delete"},
            info = {"删除这个组件，该组件引用的其它组件不会被删除。注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this component, other components referenced by this component will not be deleted. Note: Please operate with caution, it cannot be recovered after deletion."})
    public void delete(Request request, Response response) throws Exception {
        DataStore dataStore = instance.getDataStore();
        dataStore.deleteDataById(request.getId());
    }

    @ModelAction(
            name = {"创建", "en:Create"},
            info = {"获得创建该组件的默认数据或界面。", "en:Get the default data or interface for creating this component."})
    public void create(Request request, Response response) throws Exception {
        Map<String, String> properties = getAppInfo().getModelInfo(request.getModel()).getFormFieldDefaultValues();
        response.addData(properties);
    }

    @ModelAction(
            ajax = true,
            name = {"添加", "en:Add"},
            info = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    public void add(Request request, Response response) throws Exception {
        Map<String, String> properties = prepareParameters(request);
        String id = properties.get(Listable.FIELD_NAME_ID);
        DataStore dataStore = instance.getDataStore();
        dataStore.addData(id, properties);
    }
}
