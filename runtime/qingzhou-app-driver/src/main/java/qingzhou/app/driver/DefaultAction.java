package qingzhou.app.driver;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;

import qingzhou.api.*;
import qingzhou.api.type.*;
import qingzhou.crypto.Base64Coder;
import qingzhou.crypto.Crypto;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelField;

/**
 * 被 AppStubLocalImpl#invokeAction 方法反射调用
 */
public class DefaultAction {
    private DefaultAction() {
    }

    @ModelAction(
            code = Add.ACTION_CODE_CREATE, icon = "Plus", order = 1,
            name = {"新增", "en:Create"}, list_head = true,
            info = {"新增一条记录。", "en:Create a new record."})
    public static void create(Add add, Request request) {
    }

    @ModelAction(
            code = Add.ACTION_CODE_ADD, icon = "Check", order = 1,
            name = {"保存", "en:Add"}, add = true,
            info = {"保存新建的记录。", "en:Save the newly created record."})
    public static void add(Add add, Request request) throws Exception {
        Map<String, String> saveData = toSaveData(request, modelField -> modelField.add);
        add.add(request, saveData);
    }

    @ModelAction(
            code = Update.ACTION_CODE_EDIT, icon = "Edit", order = 1,
            name = {"编辑", "en:Edit"}, list = true,
            info = {"编辑本条记录。", "en:Edit this record."})
    public static void edit(Update update, Request request) {
    }

    @ModelAction(
            code = Update.ACTION_CODE_UPDATE, icon = "Check", order = 1,
            name = {"更新", "en:Update"}, update = true,
            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public static void update(Update update, Request request) throws Exception {
        Map<String, String> saveData = toSaveData(request, modelField -> modelField.update);

        String[] idFields = selectFormFields(request, field -> field.id);
        if (idFields.length > 0) {
            saveData.remove(idFields[0]);
        }
        update.update(request, saveData);
    }

    @ModelAction(
            code = Show.ACTION_CODE_SHOW, icon = "Document",
            name = {"查看", "en:Show"}, list = true, show = true,
            info = {"查看该组件的相关信息。", "en:View the information of this model."})
    public static void show(Show show, Request request) throws Exception {
        Map<String, String> showData = show.show(request);
        ResponseImpl response = (ResponseImpl) request.getResponse();
        if (response.getData() == null && showData != null) {
            String[] showFields = selectFormFields(request, modelField -> modelField.show);
            response.data(filterMapData(showData, showFields));
        }
    }

    @ModelAction(
            code = Monitor.ACTION_CODE_MONITOR, icon = "TrendCharts",
            name = {"监视", "en:Monitor"},
            info = {"查看该组件的相关信息。", "en:View the information of this model."})
    public static void monitor(Monitor monitor, Request request) throws Exception {
        Map<String, String> monitorData = monitor.monitor(request);
        ResponseImpl response = (ResponseImpl) request.getResponse();
        if (response.getData() == null && monitorData != null) {
            String[] monitorFields = selectMonitoringFields(request, modelField -> true);
            response.data(filterMapData(monitorData, monitorFields));
        }
    }

    @ModelAction(
            code = qingzhou.api.type.List.ACTION_CODE_LIST, icon = "List",
            name = {"列表", "en:List"},
            info = {"展示该类型的所有组件数据或界面。", "en:Show all component data or interfaces of this type."})
    public static void list(qingzhou.api.type.List list, Request request) throws Exception {
        Map<String, String> query = new HashMap<>();
        for (String search : selectFormFields(request, modelField -> modelField.search)) {
            String parameter = request.getParameter(search);
            if (parameter != null && !parameter.isEmpty()) {
                query.put(search, parameter);
            }
        }

        String[] showFields = selectFormFields(request, modelField -> modelField.list);
        int pageNum = parsePageParam(request.getParameter("pageNum"), 1);
        int pageSize = Math.min(parsePageParam(request.getParameter("pageSize"), 10), 100);
        List<String[]> listData = list.list(request, pageNum, pageSize, query, showFields);
        int totalSize = list.totalSize(query);
        ResponseImpl response = (ResponseImpl) request.getResponse();
        if (response.getData() == null && listData != null) {
            Map<String, Object> finalResult = new HashMap<>();
            List<Map<String, String>> listResult = new ArrayList<>();
            listData.forEach(data -> {
                Map<String, String> dataMap = new HashMap<>(); // 远程 json 序列化会丢掉顺序
                for (int i = 0; i < showFields.length; i++) {
                    dataMap.put(showFields[i], data[i]);
                }
                listResult.add(dataMap);
            });
            finalResult.put("data", listResult);
            finalResult.put("pageNum", pageNum);
            finalResult.put("pageSize", pageSize);
            finalResult.put("totalSize", totalSize);
            response.data(finalResult);
        }
    }

    @ModelAction(
            code = Delete.ACTION_CODE_DELETE, icon = "Delete", order = 99,
            name = {"删除", "en:Delete"}, list = true, batch = true,
            confirm = {"确认删除该记录？此操作不可恢复。", "en:Confirm delete this record? This action cannot be undone."},
            info = {"删除该模块。", "en:Delete this module."})
    public static void delete(Delete delete, Request request) throws Exception {
        String id = request.getId();
        if (id != null && !id.isEmpty()) {
            delete.delete(id);
        }

        String ids = request.getParameter("ids");
        if (ids != null && !ids.isEmpty()) {
            String[] idArray = ids.split(",");
            for (String itemId : idArray) {
                if (itemId != null && !itemId.isEmpty()) {
                    delete.delete(itemId);
                }
            }
        }
    }

    @ModelAction(
            code = DownloadFile.ACTION_CODE_FILES, icon = "Download", order = 3,
            name = {"下载", "en:Download"}, list_head = true, show = true, update = true,
            info = {"获取可下载的文件列表。", "en:Get the list of downloadable files."})
    public static void files(DownloadFile downloadFile, Request request) throws Exception {
        ResponseImpl response = (ResponseImpl) request.getResponse();
        if (response.getData() == null && downloadFile != null) {
            Map<String, String> map = new TreeMap<>();

            File fileBase = downloadFile.parent(request);
            if (fileBase.isDirectory()) {
                File[] files = fileBase.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && !file.getName().startsWith(".")) {
                            map.put(file.getName(), String.valueOf(FileUtil.getFileLength(file)));
                        }
                    }
                }
            } else if (fileBase.isFile()) {
                map.put(fileBase.getName(), String.valueOf(FileUtil.getFileLength(fileBase)));
            }

            response.data(map);
        }
    }

    @ModelAction(
            code = DownloadFile.ACTION_CODE_DOWNLOAD, icon = "Download",
            name = {"下载", "en:Download"},
            info = {"下载选择的文件列表。", "en:Download the selected file list."})
    public static void download(DownloadFile downloadFile, Request request) throws Exception {
        AppContext appContext = downloadFile.getAppContext();
        ResponseImpl response = (ResponseImpl) request.getResponse();

        File fileBase = downloadFile.parent(request);
        File tempBase = new File(appContext.getTemp(), "download");

        String downloadKey = request.getParameter(DownloadFile.REQUEST_PARAMETER_SERIAL_KEY);
        if (downloadKey == null || downloadKey.isEmpty()) {
            String downloadFileNames = request.getParameter(DownloadFile.REQUEST_PARAMETER_FILE_NAMES);
            if (downloadFileNames == null
                    || downloadFileNames.isEmpty()
                    || downloadFileNames.contains("..")
                    || downloadFileNames.contains(File.separator)) {
                response.msg("illegal file name");
                response.msgLevel(Response.MsgLevel.warn);
                response.success(false);
                return;
            }
            java.util.List<File> downloadFiles = new ArrayList<>();
            for (String s : downloadFileNames.split(",")) {
                File file = new File(fileBase, s);
                if (file.isFile()) {
                    downloadFiles.add(file);
                }
            }
            if (!downloadFiles.isEmpty()) {
                if (downloadFiles.size() == 1) {
                    downloadKey = downloadFiles.get(0).getName();
                } else {
                    downloadKey = buildDownloadKey(downloadFiles, tempBase);
                }
            }
        }
        if (downloadKey == null || downloadKey.isEmpty()) return;

        long offset = 0;
        String downloadOffsetParameter = request.getParameter(DownloadFile.REQUEST_PARAMETER_OFFSET);
        if (downloadOffsetParameter != null && !downloadOffsetParameter.isEmpty()) {
            offset = Long.parseLong(downloadOffsetParameter);
        }
        if (offset < 0) return;

        byte[] byteRead;

        boolean isTempFile = false;
        File toDownloadFile = new File(fileBase, downloadKey); // 单文件直接下载
        if (!toDownloadFile.exists()) {
            toDownloadFile = new File(tempBase, downloadKey); // 多文件压缩下载
            isTempFile = true;
        }
        try (RandomAccessFile raf = new RandomAccessFile(toDownloadFile, "r")) {
            if (offset >= raf.length()) return;
            raf.seek(offset);

            byte[] block = new byte[1024 * 1024 * 2]; // 一次最大传输 MB
            int read = raf.read(block);
            if (read == block.length) {
                byteRead = block;
            } else {
                byteRead = new byte[read];
                System.arraycopy(block, 0, byteRead, 0, read);
            }

            offset = raf.getFilePointer();
            if (offset == raf.length()) {
                offset = -1L; // 结束
                if (isTempFile) {
                    FileUtil.forceDeleteQuietly(toDownloadFile);
                }
            }
        }
        Crypto crypto = appContext.getService(Crypto.class);
        Base64Coder base64Coder = crypto.getBase64Coder();
        String byteEncoded = base64Coder.encode(byteRead);

        Map<String, String> data = new HashMap<>();
        data.put(DownloadFile.REQUEST_PARAMETER_SERIAL_KEY, downloadKey);
        data.put(DownloadFile.REQUEST_PARAMETER_OFFSET, String.valueOf(offset));
        data.put(DownloadFile.REQUEST_PARAMETER_BYTES, byteEncoded);
        response.data(data);
    }

    private static Map<String, String> filterMapData(Map<String, String> data, String[] useFields) {
        Map<String, String> filteredMap = new HashMap<>();
        for (String useField : useFields) {
            String val = data.get(useField);
            if (val != null) {
                filteredMap.put(useField, val);
            }
        }
        return filteredMap;
    }

    private static Map<String, String> toSaveData(Request request, Predicate<ModelField> predicate) {
        Map<String, String> data = new HashMap<>();
        for (String field : selectFormFields(request, predicate)) {
            String parameter = request.getParameter(field);
            if (parameter != null) {
                data.put(field, parameter);
            }
        }
        return data;
    }

    private static int parsePageParam(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            int result = Integer.parseInt(value);
            return result > 0 ? result : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String[] selectFormFields(Request request, Predicate<ModelField> predicate) {
        RequestImpl requestImpl = (RequestImpl) request;
        Model currentModel = requestImpl.getCurrentModel();
        return currentModel.fields.stream().filter(modelField -> modelField.field_type == FieldType.FORM).filter(predicate).map(mf -> mf.code).toArray(String[]::new);
    }

    private static String[] selectMonitoringFields(Request request, Predicate<ModelField> predicate) {
        RequestImpl requestImpl = (RequestImpl) request;
        Model currentModel = requestImpl.getCurrentModel();
        return currentModel.fields.stream().filter(modelField -> modelField.field_type == FieldType.MONITORING).filter(predicate).map(mf -> mf.code).toArray(String[]::new);
    }

    // 为支持大文件续传，下载必需有 key
    private static String buildDownloadKey(java.util.List<File> downloadFiles, File keyDir) throws IOException {
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
                    new File(keyDir, historicalKey).delete();
                }
            }
        }

        String key = format.format(new Date()) + keySP + UUID.randomUUID().toString().replace("-", "");
        File zipTo = new File(keyDir, key);
        File tempDir = new File(keyDir, UUID.randomUUID().toString());// 保障压缩文件的层次结构
        try {
            for (File file : downloadFiles) {
                FileUtil.copyFileOrDirectory(file, new File(tempDir, file.getName()));
            }
            FileUtil.zipFiles(tempDir, zipTo, false);
        } finally {
            FileUtil.forceDelete(tempDir);
        }
        return key;
    }
}
