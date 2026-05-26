package qingzhou.agent.embedded.driver;

import qingzhou.api.*;
import qingzhou.api.type.*;
import qingzhou.crypto.Base64Coder;
import qingzhou.crypto.Crypto;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
import qingzhou.dto.meta.annotation.Model;
import qingzhou.dto.meta.annotation.ModelField;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;

/**
 * 被 AppStubLocalImpl#invokeAction 方法反射调用
 */
public class DefaultAction {
    private DefaultAction() {
    }

    public static void create(Add add, Request request) {
    }

    public static void add(Add add, Request request) throws Exception {
        Map<String, String> saveData = toSaveData(request, modelField -> modelField.add);
        add.add(request, saveData);
    }

    public static void edit(Update update, Request request) {
    }

    public static void update(Update update, Request request) throws Exception {
        Map<String, String> saveData = toSaveData(request, modelField -> modelField.update);
        String[] idFields = selectFormFields(request, field -> field.id);
        if (idFields.length > 0) {
            saveData.remove(idFields[0]);
        }
        update.update(request, saveData);
    }

    public static void show(Show show, Request request) throws Exception {
        Map<String, String> showData = show.show(request);
        ResponseImpl response = (ResponseImpl) request.getResponse();
        if (response.getData() == null && showData != null) {
            String[] showFields = selectFormFields(request, modelField -> modelField.show);
            response.data(filterMapData(showData, showFields));
        }
    }

    public static void monitor(Monitor monitor, Request request) throws Exception {
        Map<String, String> monitorData = monitor.monitor(request);
        ResponseImpl response = (ResponseImpl) request.getResponse();
        if (response.getData() == null && monitorData != null) {
            String[] monitorFields = selectMonitoringFields(request, modelField -> true);
            response.data(filterMapData(monitorData, monitorFields));
        }
    }

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
                Map<String, String> dataMap = new HashMap<>();
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

    public static void files(DownloadFile downloadFile, Request request) throws Exception {
        ResponseImpl response = (ResponseImpl) request.getResponse();
        if (response.getData() == null && downloadFile != null) {
            Map<String, String> map = new TreeMap<>();
            File fileBase = downloadFile.parent(request);
            if (fileBase.isDirectory()) {
                File[] files = fileBase.listFiles();
                if (files != null) {
                    for (File file : files) {
                        map.put(file.getName(), FileUtil.getFileSize(file));
                    }
                }
            } else if (fileBase.isFile()) {
                map.put(fileBase.getName(), FileUtil.getFileSize(fileBase));
            }
            response.data(map);
        }
    }

    public static void download(DownloadFile downloadFile, Request request) throws Exception {
        AppContext appContext = downloadFile.getAppContext();
        ResponseImpl response = (ResponseImpl) request.getResponse();

        File keyDir = new File(appContext.getTemp(), "download");
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
            File fileBase = downloadFile.parent(request);
            List<File> downloadFiles = new ArrayList<>();
            for (String s : downloadFileNames.split(",")) {
                downloadFiles.add(new File(fileBase, s));
            }
            downloadKey = buildDownloadKey(downloadFiles, keyDir);
        }
        if (downloadKey.isEmpty() || !new File(keyDir, downloadKey).exists()) return;

        long offset = 0;
        String downloadOffsetParameter = request.getParameter(DownloadFile.REQUEST_PARAMETER_OFFSET);
        if (downloadOffsetParameter != null && !downloadOffsetParameter.isEmpty()) {
            offset = Long.parseLong(downloadOffsetParameter);
        }
        if (offset < 0) return;

        byte[] byteRead;
        File temp = new File(keyDir, downloadKey);
        try (RandomAccessFile raf = new RandomAccessFile(temp, "r")) {
            if (offset >= raf.length()) return;
            raf.seek(offset);
            byte[] block = new byte[1024 * 1024 * 5];
            int read = raf.read(block);
            if (read == block.length) {
                byteRead = block;
            } else {
                byteRead = new byte[read];
                System.arraycopy(block, 0, byteRead, 0, read);
            }
            offset = raf.getFilePointer();
            if (offset == raf.length()) {
                offset = -1L;
                FileUtil.forceDeleteQuietly(temp);
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
        if (value == null || value.isEmpty()) return defaultValue;
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
        return currentModel.fields.stream()
                .filter(modelField -> modelField.field_type == FieldType.FORM)
                .filter(predicate).map(mf -> mf.code)
                .toArray(String[]::new);
    }

    private static String[] selectMonitoringFields(Request request, Predicate<ModelField> predicate) {
        RequestImpl requestImpl = (RequestImpl) request;
        Model currentModel = requestImpl.getCurrentModel();
        return currentModel.fields.stream()
                .filter(modelField -> modelField.field_type == FieldType.MONITORING)
                .filter(predicate).map(mf -> mf.code)
                .toArray(String[]::new);
    }

    private static String buildDownloadKey(List<File> downloadFiles, File keyDir) throws IOException {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        String keySP = "-";
        String[] list = keyDir.list();
        if (list != null) {
            for (String historicalKey : list) {
                boolean delete = false;
                try {
                    int i = historicalKey.indexOf(keySP);
                    long time = format.parse(historicalKey.substring(0, i)).getTime();
                    if (System.currentTimeMillis() - time > 1000 * 60 * 60 * 24 * 7) {
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
        File tempDir = new File(keyDir, UUID.randomUUID().toString());
        try {
            for (File file : downloadFiles) {
                if (file.exists()) {
                    File copyTo = new File(tempDir, file.getName());
                    FileUtil.copyFileOrDirectory(file, copyTo);
                }
            }
            File[] files = tempDir.listFiles();
            if (files != null && files.length > 0) {
                FileUtil.zipFiles(tempDir, zipTo, false);
            }
        } finally {
            FileUtil.forceDelete(tempDir);
        }
        return key;
    }
}