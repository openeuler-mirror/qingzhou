package qingzhou.deployer.impl;

import qingzhou.api.*;
import qingzhou.api.type.List;
import qingzhou.api.type.*;
import qingzhou.deployer.DeployerConstants;
import qingzhou.deployer.RequestImpl;
import qingzhou.deployer.ResponseImpl;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.registry.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.*;

class DefaultAction {
    static final java.util.List<ModelActionInfo> allDefaultActionCache;

    static {
        allDefaultActionCache = DeployerImpl.parseModelActionInfos(new AnnotationReader(DefaultAction.class));
    }

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
            code = Show.ACTION_SHOW, icon = "folder-open-alt",
            name = {"查看", "en:Show"},
            info = {"查看该组件的相关信息。", "en:View the information of this model."})
    public void show(Request request) throws Exception {
        Map<String, String> data = ((Show) instance).showData(request.getId());
        request.getResponse().addData(data);
    }

    @ModelAction(
            code = Echo.ACTION_ECHO, icon = "reply",
            name = {"回显", "en:Echo"},
            info = {"处理业务逻辑上的数据级联关系。",
                    "en:Handle data cascading relationships in business logic."})
    public void echo(Request req) {
        RequestImpl request = (RequestImpl) req;
        ModelInfo modelInfo = request.getCachedModelInfo();

        Map<String, String> parameters = prepareParameters(request);

        Map<String, Map<String, String>> echoParameters = new LinkedHashMap<>();
        for (String p : parameters.keySet()) {
            String[] fieldEchoGroup = modelInfo.getModelFieldInfo(p).getEchoGroup();
            for (String group : fieldEchoGroup) {
                Map<String, String> groupParameters = echoParameters.computeIfAbsent(group, k -> new LinkedHashMap<>());
                groupParameters.put(p, parameters.get(p));
            }
        }

        Echo echo = (Echo) instance;
        Map<String, String> data = new HashMap<>();
        echoParameters.forEach((group, groupParameters) -> {
            Map<String, String> echoResult = echo.echoData(group, groupParameters);
            data.putAll(echoResult);
        });
        request.getResponse().addData(data);
    }

    @ModelAction(
            code = Option.ACTION_OPTION, icon = "check-board",
            name = {"选项", "en:Option"},
            info = {"列出指定字段的候选值。", "en:Lists the candidate values for the specified field."})
    public void option(Request request) throws Exception {
        Option option = (Option) instance;
        Item[] items = option.optionData(request.getParameter(Option.FIELD_NAME_PARAMETER));
        if (items == null) return;

        ItemInfo[] itemInfos = new ItemInfo[items.length];
        for (int i = 0; i < items.length; i++) {
            itemInfos[i] = new ItemInfo(items[i]);
        }
        ResponseImpl response = (ResponseImpl) request.getResponse();
        response.setItemInfos(itemInfos);
    }

    @ModelAction(
            code = List.ACTION_CONTAINS, icon = "search",
            name = {"查找", "en:Search"},
            info = {"展示该类型的所有组件数据或界面。", "en:Show all component data or interfaces of this type."})
    public void contains(Request request) throws Exception {
        List list = (List) instance;
        request.getResponse().setSuccess(list.contains(request.getId()));
    }

    @ModelAction(
            code = Update.ACTION_CHANGED, icon = "check-board",
            name = {"校验", "en:Changed"},
            info = {"校验指定的字段是否被改变。", "en:Verify whether the specified field has been changed."})
    public void changed(Request request) throws Exception {
        Update update = (Update) instance;
        request.getResponse().setSuccess(update.changed(request.getId(),
                request.getNonModelParameter(DeployerConstants.CHECK_KEY),
                request.getNonModelParameter(DeployerConstants.CHECK_VAL)
        ));
    }

    @ModelAction(
            code = Validate.ACTION_VALIDATE, icon = "check-board",
            name = {"校验", "en:Validate"},
            info = {"校验指定的字段是否被改变。", "en:Verify whether the specified field has been changed."})
    public void validate(Request request) throws Exception {
        Validate validate = (Validate) instance;
        Response response = request.getResponse();
        response.addData(validate.validate(request));
        response.setSuccess(response.getDataList().isEmpty());
    }

    @ModelAction(
            code = List.ACTION_ALL, icon = "list",
            name = {"列表所有", "en:List All"},
            info = {"展示该类型的所有组件数据或界面。", "en:Show all component data or interfaces of this type."})
    public void listAll(Request request) throws Exception {
        List list = (List) instance;
        Map<String, String> query = queryParams(request);
        String[] ids = list.allIds(query);
        if (ids == null) return;
        for (String id : ids) {
            request.getResponse().addData(new HashMap<String, String>() {{
                put(id, "");
            }});
        }
    }

    @ModelAction(
            code = List.ACTION_LIST, icon = "list",
            name = {"列表", "en:List"},
            info = {"展示该类型的所有组件数据或界面。", "en:Show all component data or interfaces of this type."})
    public void list(Request request) throws Exception {
        ResponseImpl responseImpl = (ResponseImpl) request.getResponse();
        int pageNum = 1;
        try {
            pageNum = Integer.parseInt(request.getNonModelParameter("pageNum"));
        } catch (NumberFormatException ignored) {
        }
        List list = (List) instance;
        int pageSize = list.pageSize();

        Map<String, String> query = queryParams(request);
        String[] fieldNamesToList = getAppInfo().getModelInfo(request.getModel()).getFieldsToList();
        java.util.List<Map<String, String>> result = list.listData(pageNum, pageSize, fieldNamesToList, query);
        if (result == null) return;

        for (Map<String, String> data : result) {
            request.getResponse().addData(data);
        }
        int totalSize = list.totalSize(query);

        responseImpl.setTotalSize(totalSize);
        responseImpl.setPageSize(pageSize);
        responseImpl.setPageNum(pageNum);
    }

    private Map<String, String> queryParams(Request request) {
        Map<String, String> query = null;
        ModelInfo modelInfo = getAppInfo().getModelInfo(request.getModel());
        for (String fieldName : modelInfo.getFieldsToListSearch()) {
            String val = request.getParameter(fieldName);
            if (Utils.notBlank(val)) {
                if (query == null) query = new HashMap<>();
                query.put(fieldName, val);
            }
        }

        return query != null ? query : ((List) instance).filterValues();
    }

    @ModelAction(
            code = Add.ACTION_CREATE, icon = "plus-sign",
            name = {"创建", "en:Create"},
            info = {"获得创建该组件的默认数据或界面。", "en:Get the default data or interface for creating this component."})
    public void create(Request request) throws Exception {
        Map<String, String> properties = getAppInfo().getModelInfo(request.getModel()).getFormFieldDefaultValues();
        request.getResponse().addData(properties);
    }

    @ModelAction(
            code = Add.ACTION_ADD, icon = "save",
            distribute = true,
            name = {"添加", "en:Add"},
            info = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    public void add(Request request) throws Exception {
        Map<String, String> properties = prepareParameters(request);
        if (request.getResponse().isSuccess()) {
            ((Add) instance).addData(properties);
        }
    }

    @ModelAction(
            code = Update.ACTION_EDIT, icon = "edit",
            name = {"编辑", "en:Edit"},
            info = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
    public void edit(Request request) throws Exception {
        Map<String, String> data = ((Update) instance).editData(request.getId());
        request.getResponse().addData(data);
    }

    @ModelAction(
            code = Update.ACTION_UPDATE, icon = "save",
            distribute = true,
            name = {"更新", "en:Update"},
            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request) throws Exception {
        Map<String, String> properties = prepareParameters(request);
        if (request.getResponse().isSuccess()) {
            ((Update) instance).updateData(properties);
        }
    }

    @ModelAction(
            code = Delete.ACTION_DELETE, icon = "trash",
            redirect = List.ACTION_LIST,
            distribute = true,
            name = {"删除", "en:Delete"},
            info = {"删除本条数据，注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this data, note: Please operate with caution, it cannot be restored after deletion."})
    public void delete(Request request) throws Exception {
        ((Delete) instance).deleteData(request.getId());
    }

    @ModelAction(
            code = Export.ACTION_EXPORT, icon = "download-alt",
            name = {"导出", "en:Export"},
            info = {"导出指定的文件流。", "en:Export the specified file stream."})
    public void export(Request request) {
        Export stream = (Export) instance;
        Export.StreamSupplier streamSupplier = stream.exportData(request.getId());
        if (streamSupplier == null) return;

        downloadStream(request, streamSupplier);
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
            code = Monitor.ACTION_MONITOR, icon = "line-chart",
            name = {"监视", "en:Monitor"},
            info = {"获取该组件的运行状态信息，该信息可反映组件的健康情况。",
                    "en:Obtain the operating status information of the component, which can reflect the health of the component."})
    public void monitor(Request request) {
        Map<String, String> p = ((Monitor) instance).monitorData(request.getId());
        if (p == null || p.isEmpty()) return;

        Map<String, String> monitorData = new HashMap<>();
        Map<String, String> infoData = new HashMap<>();

        ModelInfo modelInfo = getAppInfo().getModelInfo(request.getModel());
        String[] monitorFieldNames = modelInfo.getMonitorFieldNames();
        for (String fieldName : monitorFieldNames) {
            String val = p.get(fieldName);
            if (val == null) continue;
            ModelFieldInfo monitorField = modelInfo.getModelFieldInfo(fieldName);
            if (monitorField.isNumeric()) {
                monitorData.put(fieldName, val);
            } else {
                infoData.put(fieldName, val);
            }
        }

        request.getResponse().addData(monitorData);
        request.getResponse().addData(infoData);
    }

    @ModelAction(
            code = Download.ACTION_FILES, icon = "download-alt",
            name = {"下载", "en:Download"},
            info = {"获取该组件可下载文件的列表。",
                    "en:Gets a list of downloadable files for this component."})
    public void files(Request request) {
        String id = request.getId();
        if (id != null && id.contains("..")) {
            throw new IllegalArgumentException();
        }
        File fileBase = ((Download) instance).downloadData(id);
        if (!fileBase.isDirectory()) return;
        File[] files = fileBase.listFiles();
        if (files == null) return;

        Map<String, String> map = new LinkedHashMap<>();
        for (File rootFile : files) {
            String downloadItem = rootFile.getName();
            if (rootFile.isFile()) {
                map.put(downloadItem, downloadItem + " (" + FileUtil.getFileSize(rootFile) + ")");
            }
        }
        for (File rootFile : files) {
            if (rootFile.isDirectory()) {
                File[] subFiles = rootFile.listFiles();
                if (subFiles != null) {
                    Arrays.sort(subFiles, Comparator.comparing(File::getName));
                    for (File subFile : subFiles) {
                        map.put(rootFile.getName() + DeployerConstants.DOWNLOAD_FILE_GROUP_SP + subFile.getName(),
                                subFile.getName() + " (" + FileUtil.getFileSize(subFile) + ")");
                    }
                }
            }
        }
        request.getResponse().addData(map);
    }

    @ModelAction(
            code = Download.ACTION_DOWNLOAD, icon = "download-alt",
            name = {"下载文件", "en:Download File"},
            info = {"下载指定的文件集合，这些文件须在该组件的可下载文件列表内。",
                    "en:Downloads the specified set of files that are in the component list of downloadable files."})
    public void download(Request request) throws Exception {
        ResponseImpl response = (ResponseImpl) request.getResponse();
        File keyDir = new File(app.getAppContext().getTemp(), "download");

        String downloadKey = request.getNonModelParameter(DeployerConstants.DOWNLOAD_SERIAL_KEY);
        if (downloadKey == null || downloadKey.trim().isEmpty()) {
            String downloadFileNames = request.getNonModelParameter(DeployerConstants.DOWNLOAD_FILE_NAMES);

            // check
            if (downloadFileNames == null || downloadFileNames.trim().isEmpty()) {
                response.setMsg("No file name found.");
                response.setSuccess(false);
                return;
            }
            if (downloadFileNames.contains("..")) throw new IllegalArgumentException();

            File fileBase = ((Download) instance).downloadData(request.getId());
            java.util.List<File> downloadFiles = new ArrayList<>();
            for (String s : downloadFileNames.split(DeployerConstants.DOWNLOAD_FILE_NAMES_SP)) {
                downloadFiles.add(FileUtil.newFile(fileBase, s));//防止路径穿越：FileUtil.newFile
            }
            downloadKey = buildDownloadKey(downloadFiles, keyDir);
        }
        if (downloadKey.trim().isEmpty() || !new File(keyDir, downloadKey).exists()) return;

        response.setDownloadName(downloadKey + ".zip");
        String finalDownloadKey = downloadKey;
        downloadStream(request, new Export.StreamSupplier() {
            private final File temp = FileUtil.newFile(keyDir, finalDownloadKey);
            private final RandomAccessFile raf = new RandomAccessFile(temp, "r");

            @Override
            public int read(byte[] block, long offset) throws IOException {
                raf.seek(offset);
                return raf.read(block);
            }

            @Override
            public String serialKey() {
                return finalDownloadKey;
            }

            @Override
            public void finished() {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    FileUtil.forceDelete(temp);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // 为支持大文件续传，下载必需有 key
    private String buildDownloadKey(java.util.List<File> downloadFiles, File keyDir) throws IOException {
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

    private void downloadStream(Request request, Export.StreamSupplier supplier) {
        ResponseImpl response = (ResponseImpl) request.getResponse();

        long offset = 0;
        String downloadOffsetParameter = request.getNonModelParameter(DeployerConstants.DOWNLOAD_OFFSET);
        if (downloadOffsetParameter != null && !downloadOffsetParameter.trim().isEmpty()) {
            offset = Long.parseLong(downloadOffsetParameter.trim());
        }
        if (offset < 0) return;

        Map<String, String> result = response.getParameters();
        result.put(DeployerConstants.DOWNLOAD_SERIAL_KEY, supplier.serialKey());

        byte[] block = new byte[DeployerConstants.DOWNLOAD_BLOCK_SIZE];
        int read = 0;
        try {
            read = supplier.read(block, offset);
            if (read > 0) { // ==0 表示上次正好读取到结尾
                offset += read;

                byte[] byteRead;
                if (read == block.length) {
                    byteRead = block;
                } else {
                    byteRead = new byte[read];
                    System.arraycopy(block, 0, byteRead, 0, read);
                }
                response.setBodyBytes(byteRead);
            }
        } catch (Exception e) {
            supplier.finished();
            e.printStackTrace();
        } finally {
            if (read != block.length) {
                offset = -1L;
                supplier.finished();
            }
        }

        result.put(DeployerConstants.DOWNLOAD_OFFSET, String.valueOf(offset));
    }
}
