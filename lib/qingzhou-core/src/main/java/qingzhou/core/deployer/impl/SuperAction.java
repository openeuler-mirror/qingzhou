package qingzhou.core.deployer.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import qingzhou.api.*;
import qingzhou.api.type.List;
import qingzhou.api.type.*;
import qingzhou.core.*;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.deployer.ResponseImpl;
import qingzhou.core.registry.AppInfo;
import qingzhou.core.ItemData;
import qingzhou.core.registry.ModelActionInfo;
import qingzhou.core.registry.ModelInfo;
import qingzhou.crypto.Base64Coder;
import qingzhou.crypto.CryptoService;
import qingzhou.engine.util.FileUtil;
import qingzhou.engine.util.Utils;
import qingzhou.logger.Logger;

class SuperAction {
    static final java.util.List<ModelActionInfo> ALL_SUPER_ACTION_CACHE;

    static {
        ALL_SUPER_ACTION_CACHE = DeployerImpl.parseModelActionInfos(new AnnotationReader(SuperAction.class));
    }

    private final AppManagerImpl app;
    private final ModelBase instance;

    SuperAction(AppManagerImpl app, ModelBase instance) {
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
        ResponseImpl response = (ResponseImpl) request.getResponse();
        response.setInternalData(new HashMap<>(data));
    }

    @ModelAction(
            code = Combined.ACTION_COMBINED, icon = "folder-open-alt",
            name = {"组合查看", "en:Combined View"},
            info = {"查看组合视图的相关信息。", "en:View the related information for the composite view."})
    public void combined(Request request) throws Exception {
        ResponseImpl response = (ResponseImpl) request.getResponse();
        CombinedDataBuilder dataBuilder = new CombinedDataBuilder();
        ((Combined) instance).combinedData(request.getId(), dataBuilder);

        Object service = null; // 没有安装时，类会找不到，故使用 Object 类型
        try {
            service = app.getAppContext().getService(qingzhou.uml.Uml.class);
        } catch (Throwable e) {
            app.getAppContext().getService(Logger.class).error("The UML Generator service is not installed: " + e.getMessage());
        }

        Iterator<Combined.CombinedData> iterator = dataBuilder.data.iterator();
        while (iterator.hasNext()) {
            Combined.CombinedData data = iterator.next();
            if (data instanceof Combined.UmlData) {
                if (service == null) {
                    iterator.remove();
                    continue;
                }
                CombinedDataBuilder.Uml umlData = (CombinedDataBuilder.Uml) data;
                Base64Coder base64Coder = app.getAppContext().getService(CryptoService.class).getBase64Coder();
                qingzhou.uml.Uml uml = (qingzhou.uml.Uml) service;
                umlData.setData(base64Coder.encode(uml.toSvg(umlData.data)));
            }
        }

        response.setData(dataBuilder);
    }

    @ModelAction(
            code = Echo.ACTION_ECHO, icon = "reply",
            name = {"回显", "en:Echo"},
            info = {"处理业务逻辑上的数据级联关系。",
                    "en:Handle data cascading relationships in business logic."})
    public void echo(Request req) {
        RequestImpl request = (RequestImpl) req;
        ModelInfo modelInfo = request.getCachedModelInfo();

        Map<String, Map<String, String>> echoParameters = new LinkedHashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String p = parameterNames.nextElement();
            String[] fieldEchoGroup = modelInfo.getModelFieldInfo(p).getEchoGroup();
            for (String group : fieldEchoGroup) {
                Map<String, String> groupParameters = echoParameters.computeIfAbsent(group, k -> new LinkedHashMap<>());
                groupParameters.put(p, request.getParameter(p));
            }
        }

        ResponseImpl response = (ResponseImpl) request.getResponse();
        Echo echo = (Echo) instance;
        EchoDataBuilder dataBuilder = new EchoDataBuilder();
        for (Map.Entry<String, Map<String, String>> entry : echoParameters.entrySet()) {
            String group = entry.getKey();
            Map<String, String> groupParameters = entry.getValue();
            echo.echoData(group, groupParameters, dataBuilder);
        }
        response.setData(dataBuilder);
    }

    @ModelAction(
            code = Option.ACTION_OPTION, icon = "check-board",
            name = {"选项", "en:Option"},
            info = {"列出指定字段的候选值。", "en:Lists the candidate values for the specified field."})
    public void option(Request request) throws Exception {
        Option option = (Option) instance;
        Item[] items = option.optionData(request.getId(), request.getParameter(DeployerConstants.DYNAMIC_OPTION_FIELD));
        if (items == null) return;

        ItemData[] itemData = new ItemData[items.length];
        for (int i = 0; i < items.length; i++) {
            itemData[i] = new ItemData(items[i]);
        }
        ResponseImpl response = (ResponseImpl) request.getResponse();
        response.setInternalData(itemData);
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
            code = Validate.ACTION_VALIDATE, icon = "check-board",
            name = {"校验", "en:Validate"},
            info = {"校验指定的字段是否被改变。", "en:Verify whether the specified field has been changed."})
    public void validate(Request request) throws Exception {
        Validate validate = (Validate) instance;
        ResponseImpl response = (ResponseImpl) request.getResponse();
        boolean isAdd = Boolean.parseBoolean(request.getParameter(DeployerConstants.VALIDATION_ADD_FLAG));
        Map<String, String> errors = validate.validate(request, new Validate.ValidationContext() {
            @Override
            public boolean isAdd() {
                return isAdd;
            }

            @Override
            public boolean isUpdate() {
                return !isAdd;
            }
        });
        response.setInternalData(new HashMap<>(errors));
        response.setSuccess(errors.isEmpty());
    }

    @ModelAction(
            code = List.ACTION_ALL, icon = "list",
            name = {"列出所有", "en:List All"},
            info = {"获取该模块的所有ID数据。", "en:Get all the ID data for the module."})
    public void all(Request request) throws Exception {
        List list = (List) instance;
        String parameter = request.getParameter(DeployerConstants.LIST_ALL_FIELDS);
        String idField = ((RequestImpl) request).getCachedModelInfo().getIdField();
        String[] getFields = Utils.notBlank(parameter) ? new String[]{idField, parameter} : new String[]{idField};
        java.util.ArrayList<String[]> result = new ArrayList<>(list.listData(1, list.maxResponseDataSize(), getFields, null));
        ResponseImpl response = (ResponseImpl) request.getResponse();
        response.setInternalData(result);
    }

    @ModelAction(
            code = List.ACTION_LIST, icon = "list",
            name = {"列表", "en:List"},
            info = {"展示该类型的所有组件数据或界面。", "en:Show all component data or interfaces of this type."})
    public void list(Request request) throws Exception {
        ResponseImpl responseImpl = (ResponseImpl) request.getResponse();
        int pageNum = 1;
        try {
            pageNum = Integer.parseInt(request.getParameter(ListData.PAGE_NUM));
        } catch (NumberFormatException ignored) {
        }
        List list = (List) instance;
        int pageSize = Math.min(list.pageSize(), list.maxResponseDataSize());

        Map<String, String> query = queryParams(request);
        String[] fieldNamesToList = getAppInfo().getModelInfo(request.getModel()).getFieldsToList();
        java.util.List<String[]> result = list.listData(pageNum, pageSize, fieldNamesToList, query);
        if (result == null) return;

        ListData listData = new ListData();
        listData.dataList = result;
        listData.totalSize = list.totalSize(query);
        listData.pageSize = pageSize;
        listData.pageNum = pageNum;
        responseImpl.setInternalData(listData);
    }

    private Map<String, String> queryParams(Request request) {
        Map<String, String> query = null;
        ModelInfo modelInfo = getAppInfo().getModelInfo(request.getModel());
        for (String fieldName : modelInfo.getFieldsToListSearch()) {
            String val = request.getParameter(fieldName);
            if (val != null) { // 注意不要用  “” 判定，以区分使用默认搜索，还是 清空所有条件！！！
                if (query == null) query = new HashMap<>();
                query.put(fieldName, val);
            }
        }

        if (query != null) return query;

        if (instance instanceof List) return ((List) instance).defaultSearch();

        return null;
    }

    @ModelAction(
            code = Add.ACTION_CREATE, icon = "plus-sign",
            head_action = true, order = "1",
            name = {"创建", "en:Create"},
            info = {"获得创建该组件的默认数据或界面。", "en:Get the default data or interface for creating this component."})
    public void create(Request request) {
        Map<String, String> properties = getAppInfo().getModelInfo(request.getModel()).getFormFieldDefaultValues();
        ResponseImpl response = (ResponseImpl) request.getResponse();
        response.setInternalData(new HashMap<>(properties));
    }

    @ModelAction(
            code = Add.ACTION_ADD, icon = "save",
            distribute = true, order = "1",
            name = {"添加", "en:Add"},
            info = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    public void add(Request request) throws Exception {
        Map<String, String> properties = ((RequestImpl) request).getParameters();
        cleanParameters(properties, request);
        ((Add) instance).addData(properties);
    }

    private void cleanParameters(Map<String, String> params, Request request) {
        String action = request.getAction();
        if (Add.ACTION_ADD.equals(action)
                || Update.ACTION_UPDATE.equals(action)) {
            ModelInfo modelInfo = getAppInfo().getModelInfo(request.getModel());
            java.util.List<String> toRemove = params.keySet().stream().filter(param -> Arrays.stream(modelInfo.getFormFieldNames()).noneMatch(s -> s.equals(param))).collect(Collectors.toList());
            toRemove.forEach(params::remove);
        }
    }

    @ModelAction(
            code = Update.ACTION_EDIT, icon = "edit",
            list_action = true, order = "1",
            name = {"编辑", "en:Edit"},
            info = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
    public void edit(Request request) throws Exception {
        Map<String, String> data = ((Update) instance).editData(request.getId());
        ResponseImpl response = (ResponseImpl) request.getResponse();
        response.setInternalData(new HashMap<>(data));
    }

    @ModelAction(
            code = Update.ACTION_UPDATE, icon = "save",
            distribute = true, order = "1",
            name = {"更新", "en:Update"},
            info = {"更新这个模块的配置信息。", "en:Update the configuration information for this module."})
    public void update(Request request) throws Exception {
        Map<String, String> properties = ((RequestImpl) request).getParameters();
        cleanParameters(properties, request);
        ((Update) instance).updateData(properties);
    }

    @ModelAction(
            code = Delete.ACTION_DELETE, icon = "trash",
            list_action = true, order = "9", action_type = ActionType.action_list, distribute = true,
            name = {"删除", "en:Delete"},
            info = {"删除本条数据，注：请谨慎操作，删除后不可恢复。",
                    "en:Delete this data, note: Please operate with caution, it cannot be restored after deletion."})
    public void delete(Request request) throws Exception {
        ((Delete) instance).deleteData(request.getId());
    }

    @ModelAction(
            code = List.ACTION_DEFAULT_SEARCH, icon = "trash",
            action_type = ActionType.action_list,
            name = {"默认检索", "en:Default Search"},
            info = {"设置列表数据默认的检索条件。", "en:Set the default search conditions for list data."})
    public void defaultSearch(Request request) {
        Map<String, String> defaultSearch = ((List) instance).defaultSearch();
        ResponseImpl response = (ResponseImpl) request.getResponse();
        response.setInternalData(new HashMap<>(defaultSearch));
    }

    @ModelAction(
            code = Export.ACTION_EXPORT, icon = "download-alt",
            name = {"导出", "en:Export"},
            head_action = true,
            action_type = ActionType.export,
            info = {"导出指定的文件流。", "en:Export the specified file stream."})
    public void export(Request request) throws Exception {
        Export stream = (Export) instance;
        Export.DataSupplier dataSupplier = stream.exportData(request.getId());
        if (dataSupplier == null) return;

        downloadStream(request, dataSupplier);
    }

    @ModelAction(
            code = Monitor.ACTION_MONITOR, icon = "line-chart",
            list_action = true, order = "2",
            name = {"监视", "en:Monitor"},
            info = {"获取该组件的运行状态信息，该信息可反映组件的健康情况。",
                    "en:Obtain the operating status information of the component, which can reflect the health of the component."})
    public void monitor(Request request) throws Exception {
        Map<String, String> p = ((Monitor) instance).monitorData(request.getId());
        if (p == null || p.isEmpty()) return;

        ResponseImpl response = (ResponseImpl) request.getResponse();
        response.setInternalData(new HashMap<>(p));
    }

    @ModelAction(
            code = Chart.ACTION_CHART, icon = "line-chart",
            name = {"监视", "en:Monitor"},
            info = {"获取该组件的历史状态信息，该信息可反映组件的健康情况。",
                    "en:Obtain the historical status information of the component, which can reflect the health status of the component."})
    public void chart(Request request) throws Exception {
        ResponseImpl response = (ResponseImpl) request.getResponse();

        Chart chart = (Chart) instance;
        ChartDataBuilder chartDataBuilder = new ChartDataBuilder();
        chart.chartData(chartDataBuilder);
        response.setData(chartDataBuilder);
    }

    @ModelAction(
            code = Download.ACTION_FILES, icon = "download-alt",
            list_action = true, order = "8",
            action_type = ActionType.download,
            name = {"下载", "en:Download"},
            info = {"获取该组件可下载文件的列表。",
                    "en:Gets a list of downloadable files for this component."})
    public void files(Request request) throws Exception {
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
                        map.put(rootFile.getName() + DownloadData.DOWNLOAD_FILE_GROUP_SP + subFile.getName(),
                                subFile.getName() + " (" + FileUtil.getFileSize(subFile) + ")");
                    }
                }
            }
        }

        ResponseImpl response = (ResponseImpl) request.getResponse();
        response.setInternalData(new HashMap<>(map));
    }

    @ModelAction(
            code = Download.ACTION_DOWNLOAD, icon = "download-alt",
            name = {"下载文件", "en:Download File"},
            info = {"下载指定的文件集合，这些文件须在该组件的可下载文件列表内。",
                    "en:Downloads the specified set of files that are in the component list of downloadable files."})
    public void download(Request request) throws Exception {
        ResponseImpl response = (ResponseImpl) request.getResponse();
        File keyDir = new File(app.getAppContext().getTemp(), "download");

        String downloadKey = request.getParameter(DownloadData.DOWNLOAD_SERIAL_KEY);
        if (downloadKey == null || downloadKey.trim().isEmpty()) {
            String downloadFileNames = request.getParameter(DownloadData.DOWNLOAD_FILE_NAMES);

            // check
            if (downloadFileNames == null || downloadFileNames.trim().isEmpty()) {
                response.setMsg("No file name found.");
                response.setSuccess(false);
                return;
            }
            if (downloadFileNames.contains("..")) throw new IllegalArgumentException();

            File fileBase = ((Download) instance).downloadData(request.getId());
            java.util.List<File> downloadFiles = new ArrayList<>();
            for (String s : downloadFileNames.split(DownloadData.DOWNLOAD_FILE_NAMES_SP)) {
                downloadFiles.add(FileUtil.newFile(fileBase, s));//防止路径穿越：FileUtil.newFile
            }
            downloadKey = buildDownloadKey(downloadFiles, keyDir);
        }
        if (downloadKey.trim().isEmpty() || !new File(keyDir, downloadKey).exists()) return;
        response.getParameters().put(DownloadData.DOWNLOAD_SERIAL_KEY, downloadKey);
        String finalDownloadKey = downloadKey;
        Export.DataSupplier supplier = new Export.DataSupplier() {
            private long currentOffset;

            @Override
            public byte[] read(long offset) throws IOException {
                File temp = FileUtil.newFile(keyDir, finalDownloadKey);
                try (RandomAccessFile raf = new RandomAccessFile(temp, "r")) {
                    currentOffset = offset;
                    raf.seek(offset);

                    byte[] byteRead;
                    byte[] block = new byte[1024 * 1024 * 5];
                    int read = raf.read(block);
                    if (read == block.length) {
                        byteRead = block;
                    } else {
                        byteRead = new byte[read];
                        System.arraycopy(block, 0, byteRead, 0, read);
                    }

                    currentOffset += read;
                    if (currentOffset != raf.getFilePointer()) throw new IllegalStateException();
                    if (currentOffset >= raf.length()) {
                        currentOffset = -1L;
                        try {
                            FileUtil.forceDelete(temp);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    return byteRead;
                } catch (Throwable e) {
                    try {
                        FileUtil.forceDelete(temp);
                    } catch (IOException ignored) {
                    }
                    throw e;
                }
            }

            @Override
            public long offset() {
                return currentOffset;
            }

            @Override
            public String name() {
                return finalDownloadKey + ".zip";
            }
        };
        downloadStream(request, supplier);
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

    private void downloadStream(Request request, Export.DataSupplier supplier) throws IOException {
        ResponseImpl response = (ResponseImpl) request.getResponse();

        long offset = 0;
        String downloadOffsetParameter = request.getParameter(DownloadData.DOWNLOAD_OFFSET);
        if (downloadOffsetParameter != null && !downloadOffsetParameter.trim().isEmpty()) {
            offset = Long.parseLong(downloadOffsetParameter.trim());
        }
        if (offset < 0) return;

        byte[] block = supplier.read(offset);
        DownloadData dd = new DownloadData();
        dd.block = block;
        dd.downloadName = supplier.name();
        response.setInternalData(dd);
        response.getParameters().put(DownloadData.DOWNLOAD_OFFSET, String.valueOf(supplier.offset()));
    }

    @ModelAction(
            code = Dashboard.ACTION_DASHBOARD, icon = "dashboard",
            name = {"监视概览", "en:Monitoring Overview"},
            info = {"获取该组件的统计概览信息。",
                    "en:Obtain the statistical overview of the component."})
    public void dashboard(Request request) {
        Dashboard instance = (Dashboard) this.instance;
        DashboardDataBuilder dashboardBuilder = new DashboardDataBuilder();

        instance.dashboardData(request.getId(), dashboardBuilder);

        dashboardBuilder.setPeriod(instance.period());

        dashboardBuilder.transformData();// 转换为前端需要的格式

        request.getResponse().setData(dashboardBuilder);
    }
}
