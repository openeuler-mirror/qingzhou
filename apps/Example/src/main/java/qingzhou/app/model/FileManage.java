package qingzhou.app.model;

import qingzhou.api.*;
import qingzhou.api.type.*;
import qingzhou.app.ExampleMain;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Model(code = "filemanage", icon = "file", menu = ExampleMain.MENU_11, order = 4, name = {"文件管理", "en:File Manage"}, info = {"对系统中的文件进行管理。", "en:Manage files in the system."})
public class FileManage extends ModelBase implements Add, Show, List, Delete, Download {
    public static final String FILE_BASEDIR = "files";

    @ModelField(required = true, create = false, edit = false, list = true, name = {"文件名称", "en:Department Name"}, info = {"该文件的名称。", "en:The name of the department."})
    public String name;

    @ModelField(type = FieldType.file, required = true, list = true, name = {"上传文件", "en:Upload File"}, info = {"上传一个文件到服务器，文件须是 *.html类型的。", "en:Upload a file to the server of type *.html."})
    public String file;

    @ModelAction(code = "showhtml", icon = "share-alt", order = 1, name = {"Html", "en:Html"}, list = true,
            page = "static/test.html", info = {"查看该组件的相关信息。", "en:View the information of this model."})
    public void showHtml(Request request) {
    }

    @Override
    public String idField() {
        return "name";
    }

    @Override
    public File downloadData(String id) {
        return new File(new File(ExampleMain.appContext.getAppDir(), FILE_BASEDIR), id);
    }

    @ModelAction(code = Add.ACTION_ADD, icon = "save", name = {"添加", "en:Add"}, info = {"按配置要求创建一个模块。", "en:Create a module as configured."})
    public void add(Request request) throws Exception {
        String file = request.getParameter("file");
        File srcFile = new File(file);
        if (!srcFile.exists()) {
            request.getResponse().setSuccess(false);
            return;
        }

        String fileName = srcFile.getName();

        File path = new File(new File(ExampleMain.appContext.getAppDir(), FILE_BASEDIR), fileName.substring(0, fileName.indexOf(".")));
        Files.createDirectories(path.toPath());
        Files.copy(srcFile.toPath(), new File(path, fileName).toPath());
    }

    @Override
    public Map<String, String> showData(String id) throws Exception {
        File path = new File(ExampleMain.appContext.getAppDir(), FILE_BASEDIR);
        if (path.exists()) {
            for (File file : path.listFiles()) {
                if (!file.isDirectory()) {
                    continue;// 这里是因为上传的时候，会以不带后缀的文件名作为目录名，所以需要过滤掉非目录
                }
                String fileName = file.getName();
                if (id.equals(fileName)) {
                    return new HashMap<String, String>() {{
                        put("name", fileName);
                        put("file", file.getAbsolutePath().replace(ExampleMain.appContext.getAppDir().getAbsolutePath(), ""));
                    }};
                }
            }
        }
        return null;
    }

    @Override
    public String[] allIds(Map<String, String> query) {
        File path = new File(ExampleMain.appContext.getAppDir(), FILE_BASEDIR);
        java.util.List<String> ids = new ArrayList<>();
        if (path.exists()) {
            for (File file : path.listFiles()) {
                if (!file.isDirectory()) {
                    continue;// 这里是因为上传的时候，会以不带后缀的文件名作为目录名，所以需要过滤掉非目录
                }
                ids.add(file.getName());
            }
        }
        return ids.toArray(new String[0]);
    }

    @Override
    public java.util.List<Map<String, String>> listData(int pageNum, int pageSize, String[] showFields, Map<String, String> query) throws Exception {
        String[] allIds = allIds(query);
        int totalSize = allIds.length;
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalSize);
        String[] subList = Arrays.copyOfRange(allIds, startIndex, endIndex);

        java.util.List<Map<String, String>> data = new ArrayList<>();
        for (String id : subList) {
            Map<String, String> result = new HashMap<>();

            Map<String, String> idData = showData(id);
            if (idData == null) {
                continue;
            }

            for (String fieldName : showFields) {
                result.put(fieldName, idData.get(fieldName));
            }

            data.add(result);
        }
        return data;
    }

    @Override
    public void addData(Map<String, String> data) throws Exception {

    }

    @Override
    public void deleteData(String id) throws Exception {
        File path = new File(ExampleMain.appContext.getAppDir(), FILE_BASEDIR);
        if (path.exists()) {
            for (File file : path.listFiles()) {
                if (!file.isDirectory()) {
                    continue;// 这里是因为上传的时候，会以不带后缀的文件名作为目录名，所以需要过滤掉非目录
                }
                String fileName = file.getName();
                if (id.equals(fileName)) {
                    Files.delete(Paths.get(file.getPath()));
                }
            }
        }
    }
}
