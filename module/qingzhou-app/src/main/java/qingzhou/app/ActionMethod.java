package qingzhou.app;

import qingzhou.api.*;
import qingzhou.api.metadata.ModelFieldData;
import qingzhou.api.metadata.ModelManager;
import qingzhou.api.type.*;
import qingzhou.framework.console.ResponseImpl;

import java.util.*;

public class ActionMethod {
    private final ModelBase modelBase;

    public ActionMethod(ModelBase modelBase) {
        this.modelBase = modelBase;
    }

    private AppContext getAppContext() {
        return modelBase.getAppContext();
    }

    private DataStore getDataStore() {
        return modelBase.getDataStore();
    }

    @ModelAction(name = Showable.ACTION_NAME_SHOW,
            icon = "info-sign", forwardToPage = "show",
            nameI18n = {"查看", "en:Show"},
            infoI18n = {"查看该组件的相关信息。", "en:View the information of this model."})
    public void show(Request request, Response response) throws Exception {
        DataStore dataStore = getDataStore();
        Map<String, String> data = dataStore.getDataById(request.getModelName(), request.getId());
        response.addData(data);
    }

    @ModelAction(name = Monitorable.ACTION_NAME_MONITOR,
            showToList = true, orderOnList = 97,
            icon = "area-chart", forwardToPage = "show",
            nameI18n = {"监视", "en:Monitor"},
            infoI18n = {"获取该组件的运行状态信息，该信息可反映组件的健康情况。",
                    "en:Obtain the operating status information of the component, which can reflect the health of the component."})
    public void monitor(Request request, Response response) {
        Map<String, String> p = ((Monitorable) modelBase).monitorData();

        if (p == null || p.isEmpty()) {
            return;
        }
        List<String> graphicalDynamicFields = new ArrayList<>();
        Map<String, String> monitorData = new HashMap<>();
        Map<String, String> infoData = new HashMap<>();
        for (Map.Entry<String, ModelFieldData> entry : getAppContext().getAppMetadata().getModelManager().getMonitorFieldMap(request.getModelName()).entrySet()) {
            String fieldName = entry.getKey();
            ModelFieldData monitorField = entry.getValue();
            if (monitorField.supportGraphicalDynamic()) {
                graphicalDynamicFields.add(fieldName);
            } else {
                String value = p.get(fieldName);
                if (value != null) {
                    if (monitorField.supportGraphicalDynamic()) {
                        graphicalDynamicFields.add(fieldName);
                    } else if (monitorField.supportGraphical()) {
                        monitorData.put(fieldName, value);
                    } else {
                        infoData.put(fieldName, value);
                    }
                }
            }
        }

        // 检查是否有待扩展属性？
        for (String check : graphicalDynamicFields) {
            for (String k : p.keySet()) {
                if (k.startsWith(check + Monitorable.MONITOR_EXT_SEPARATOR)) {
                    monitorData.put(k, p.get(k));
                }
            }
        }

        response.addData(monitorData);
        response.addData(infoData);
    }

    @ModelAction(name = Listable.ACTION_NAME_LIST,
            icon = "list", forwardToPage = "list",
            nameI18n = {"列表", "en:List"},
            infoI18n = {"展示该类型的所有组件数据或界面。", "en:Show all component data or interfaces of this type."})
    public void list(Request request, Response response) throws Exception {
        String modelName = request.getModelName();
        DataStore dataStore = getDataStore();
        if (dataStore == null) {
            return;
        }
        int totalSize = dataStore.getTotalSize(modelName);
        response.setTotalSize(totalSize);

        response.setPageSize(10);

        int pageNum = 1;
        try {
            pageNum = Integer.parseInt(request.getParameter(Listable.PARAMETER_PAGE_NUM));
        } catch (NumberFormatException ignored) {
        }
        response.setPageNum(pageNum);

        String[] dataIdInPage = dataStore.getDataIdInPage(modelName, ((ResponseImpl) response).getPageSize(), pageNum).toArray(new String[0]);
        ModelManager manager = getAppContext().getAppMetadata().getModelManager();
        String[] fieldNamesToList = Arrays.stream(manager.getFieldNames(modelName)).filter(s -> manager.getModelField(modelName, s).showToList()).toArray(String[]::new);
        List<Map<String, String>> result = dataStore.getDataFieldByIds(modelName, dataIdInPage, fieldNamesToList);
        for (Map<String, String> data : result) {
            response.addData(data);
        }
    }

    @ModelAction(name = Editable.ACTION_NAME_EDIT,
            icon = "edit", forwardToPage = "form",
            nameI18n = {"编辑", "en:Edit"},
            infoI18n = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
    public void edit(Request request, Response response) throws Exception {
        show(request, response);
    }

    @ModelAction(name = Editable.ACTION_NAME_UPDATE,
            icon = "save",
            nameI18n = {"更新", "en:Update"},
            infoI18n = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request, Response response) throws Exception {
        DataStore dataStore = getDataStore();
        Map<String, String> properties = prepareParameters(request);
        dataStore.updateDataById(request.getModelName(), request.getId(), properties);
    }

    public Map<String, String> prepareParameters(Request request) {
        Map<String, String> properties = new HashMap<>();
        String[] fieldNames = getAppContext().getAppMetadata().getModelManager().getFieldNames(request.getModelName());
        for (String fieldName : fieldNames) {
            String value = request.getParameter(fieldName);
            if (value != null) {
                properties.put(fieldName, value);
            }
        }
        return properties;
    }

    //// todo：实现移动到内部?
//    @ModelAction(name = ACTION_NAME_DOWNLOADLIST,
//            showToList = true, orderOnList = 98,
//            icon = "download-alt",
//            nameI18n = {"下载", "en:Download"},
//            infoI18n = {"获取该组件可下载文件的列表。",
//                    "en:Gets a list of downloadable files for this component."})
//    default void downloadlist(Request request, Response response) throws Exception {
//        Map<String, List<String[]>> result = downloadlist0(request, response);
//        if (result != null && !result.isEmpty()) {
//            HashMap<String, String> map = new HashMap<>();
//            if (result.size() == 1) {
//                for (String[] file : result.values().iterator().next()) {
//                    map.put(file[0], file[0] + " (" + file[1] + ")");
//                }
//            } else {
//                for (Map.Entry<String, List<String[]>> entry : result.entrySet()) {
//                    String group = entry.getKey();
//                    for (String[] file : entry.getValue()) {
//                        map.put(group + DOWNLOAD_NAME_SEPARATOR + file[0], file[0] + " (" + file[1] + ")");
//                    }
//                }
//            }
//            response.addData(map);
//        }
//    }
//
//    Map<String, List<String[]>> downloadlist0(Request request, Response response) throws Exception;
//
//    @ModelAction(name = ACTION_NAME_DOWNLOADFILE,
//            icon = "download-alt",
//            nameI18n = {"下载文件", "en:Download File"},
//            infoI18n = {"下载指定的文件集合，这些文件须在该组件的可下载文件列表内。注：指定的文件集合须以 " + PARAMETER_DOWNLOAD_FILE_NAMES + " 参数传递给服务器，多个文件之间用英文逗号分隔。",
//                    "en:Downloads the specified set of files that are in the component list of downloadable files. Note: The specified file set must be passed to the server with the " + PARAMETER_DOWNLOAD_FILE_NAMES + " parameter, and multiple files are separated by commas."})
//    default void downloadfile(Request request, Response response) throws Exception {
//        File keyDir = new File(getAppContext().getTemp(), "download");
//
//        String downloadKey = request.getParameter(DOWNLOAD_KEY);
//        if (downloadKey == null || downloadKey.trim().isEmpty()) {
//            String downloadFileNames = request.getParameter(PARAMETER_DOWNLOAD_FILE_NAMES);
//
//            // check
//            if (downloadFileNames == null || downloadFileNames.trim().isEmpty()) {
//                response.setMsg("No file name found.");
//                response.setSuccess(false);
//                return;
//            }
//
//            File[] downloadFiles = downloadfile0(request, downloadFileNames.split(DOWNLOAD_FILE_NAME_SEPARATOR));
//            downloadKey = buildDownloadKey(downloadFiles, keyDir);
//        }
//
//        long downloadOffset = 0;
//        String downloadOffsetParameter = request.getParameter(DOWNLOAD_OFFSET);
//        if (downloadOffsetParameter != null && !downloadOffsetParameter.trim().isEmpty()) {
//            downloadOffset = Long.parseLong(downloadOffsetParameter.trim());
//        }
//
//        Map<String, String> downloadedStatus = downloadFile(downloadKey, downloadOffset, keyDir);
//        if (downloadedStatus != null) {
//            response.addData(downloadedStatus);
//        }
//    }
//
//    // 计算得出需要下载的实际文件列表
//    File[] downloadfile0(Request request, String[] downloadFileNames) throws Exception;
//
//    AppContext getAppContext(); // 已在 ModelBase 里自动实现
//
//    // 为支持大文件续传，下载必需有 key
//    static Map<String, String> downloadFile(String key, long offset, File keyDir) throws Exception {
//        if (key == null || key.trim().isEmpty() || !new File(keyDir, key).exists()) {
//            return null;
//        }
//        if (offset < 0) {
//            return null;
//        }
//        Map<String, String> result = new HashMap<>();
//
//        File downloadFile = new File(keyDir, key);
//        result.put(DOWNLOAD_KEY, key);
//
//        byte[] byteRead;
//        boolean hasMore = false;
//        try (RandomAccessFile raf = new RandomAccessFile(downloadFile, "r")) {
//            raf.seek(offset);
//    int DOWNLOAD_BLOCK_SIZE = Integer.parseInt(System.getProperty("qingzhou.DOWNLOAD_BLOCK_SIZE", String.valueOf(1024 * 1024 * 2)));
//            byte[] block = new byte[DOWNLOAD_BLOCK_SIZE];
//            int read = raf.read(block);
//            if (read > 0) { // ==0 表示上次正好读取到结尾
//                if (read == block.length) {
//                    byteRead = block;
//
//                    if (raf.getFilePointer() < raf.length() - 1) {
//                        hasMore = true;
//                    }
//                } else {
//                    byteRead = new byte[read];
//                    System.arraycopy(block, 0, byteRead, 0, read);
//                }
//                result.put(DOWNLOAD_BLOCK, HexUtil.bytesToHex(byteRead));
//                offset = raf.getFilePointer();
//            }
//        }
//
//        if (hasMore) {
//            result.put(DOWNLOAD_OFFSET, String.valueOf(offset));
//        } else {
//            result.put(DOWNLOAD_OFFSET, String.valueOf(-1L));
//            File temp = FileUtil.newFile(keyDir, key);
//            FileUtil.forceDeleteQuietly(temp);
//        }
//
//        return result;
//    }
//
//    // 为支持大文件续传，下载必需有 key
//    static String buildDownloadKey(File[] downloadFiles, File keyDir) throws IOException {
//        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
//        String keySP = "-";
//
//        // 清理历史的临时文件
//        String[] list = keyDir.list();
//        if (list != null) {
//            for (String historicalKey : list) {
//                boolean delete = false;
//                try {
//                    int i = historicalKey.indexOf(keySP);
//                    long time = format.parse(historicalKey.substring(0, i)).getTime();
//                    if (System.currentTimeMillis() - time > 1000 * 60 * 60 * 24 * 7) { // 清理可能下载失败的文件
//                        delete = true;
//                    }
//                } catch (Exception e) {
//                    System.err.println("failed to parse time: " + historicalKey);
//                }
//                if (delete) {
//                    File temp = FileUtil.newFile(keyDir, historicalKey);
//                    FileUtil.forceDeleteQuietly(temp);
//                }
//            }
//        }
//
//        String key = format.format(new Date()) + keySP + UUID.randomUUID().toString().replace("-", "");
//        File zipTo = FileUtil.newFile(keyDir, key);
//        File tempDir = FileUtil.newFile(keyDir, UUID.randomUUID().toString());// 保障压缩文件的层次结构
//        for (File file : downloadFiles) {
//            if (file.exists()) {
//                File copyTo = new File(tempDir, file.getName());
//                if (file.isDirectory()) {
//                    FileUtil.mkdirs(copyTo);
//                }
//                FileUtil.copyFileOrDirectory(file, copyTo);
//            }
//        }
//        FileUtil.zipFiles(tempDir, zipTo, false);
//        FileUtil.forceDeleteQuietly(tempDir);
//        return key;
//    }

    @ModelAction(
            name = Deletable.ACTION_NAME_DELETE,
            showToList = true, orderOnList = 99,
            supportBatch = true,
            icon = "trash",
            nameI18n = {"删除", "en:Delete"},
            infoI18n = {"删除这个组件，该组件引用的其它组件不会被删除。注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this component, other components referenced by this component will not be deleted. Note: Please operate with caution, it cannot be recovered after deletion."})
    public void delete(Request request, Response response) throws Exception {

        DataStore dataStore = getDataStore();
        dataStore.deleteDataById(request.getModelName(), request.getId());
    }

    @ModelAction(name = Createable.ACTION_NAME_CREATE,
            showToListHead = true,
            icon = "plus-sign", forwardToPage = "form",
            nameI18n = {"创建", "en:Create"},
            infoI18n = {"获得创建该组件的默认数据或界面。", "en:Get the default data or interface for creating this component."})
    public void create(Request request, Response response) {
        Map<String, String> properties = getAppContext().getAppMetadata().getModelManager().getModelDefaultProperties(request.getModelName());
        response.addData(properties);
    }

    @ModelAction(name = Createable.ACTION_NAME_ADD,
            icon = "save",
            nameI18n = {"添加", "en:Add"},
            infoI18n = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    public void add(Request request, Response response) throws Exception {
        Map<String, String> properties = ((Createable) modelBase).add(request, response);
        if (properties == null || properties.isEmpty()) return;

        String id = properties.get(Listable.FIELD_NAME_ID);
        DataStore dataStore = getDataStore();
        dataStore.addData(request.getModelName(), id, properties);
    }
}
