package qingzhou.app.model;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import qingzhou.api.InputType;
import qingzhou.api.Model;
import qingzhou.api.ModelAction;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.Request;
import qingzhou.api.type.Add;
import qingzhou.api.type.Delete;
import qingzhou.api.type.Download;
import qingzhou.api.type.List;
import qingzhou.api.type.Show;
import qingzhou.app.ExampleMain;

@Model(code = "filemanage", icon = "file", menu = ExampleMain.MENU_11,
        order = "4", name = {"文件管理", "en:File Manage"}, info = {"对系统中的文件进行管理。", "en:Manage files in the system."})
public class FileManage extends ModelBase implements Add, Show, List, Delete, Download {
    public static final String FILE_BASEDIR = "files";

    @ModelField(id = true, create = false, edit = false, name = {"文件名称", "en:Department Name"}, info = {"该文件的名称。", "en:The name of the department."})
    public String id;

    @ModelField(input_type = InputType.file, required = true, list = true, name = {"上传文件", "en:Upload File"}, info = {"上传一个文件到服务器，文件须是 *.html类型的。", "en:Upload a file to the server of type *.html."})
    public String file;

    @ModelAction(code = "showhtml", icon = "share-alt", name = {"Html", "en:Html"},
            list_action = true,
            app_page = "static/test.html", info = {"查看该组件的相关信息。", "en:View the information of this model."})
    public void showHtml(Request request) {
    }

    @Override
    public File downloadData(String id) {
        return new File(new File(getAppContext().getAppDir(), FILE_BASEDIR), id);
    }

    @Override
    public Map<String, String> showData(String id) {
        File path = new File(getAppContext().getAppDir(), FILE_BASEDIR);
        if (path.exists()) {
            for (File file : path.listFiles()) {
                if (!file.isDirectory()) {
                    continue;// 这里是因为上传的时候，会以不带后缀的文件名作为目录名，所以需要过滤掉非目录
                }
                String fileName = file.getName();
                if (id.equals(fileName)) {
                    return new HashMap<String, String>() {{
                        put("id", fileName);
                        put("file", file.getAbsolutePath().replace(getAppContext().getAppDir().getAbsolutePath(), ""));
                    }};
                }
            }
        }
        return null;
    }

    @Override
    public boolean contains(String id) {
        String[] ids = allIds(null);
        for (String s : ids) {
            if (s.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private String[] allIds(Map<String, String> query) {
        File path = new File(getAppContext().getAppDir(), FILE_BASEDIR);
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
    public java.util.List<String[]> listData(int pageNum, int pageSize, String[] showFields, Map<String, String> query) throws Exception {
        String[] allIds = allIds(query);
        int totalSize = allIds.length;
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalSize);
        String[] subList = Arrays.copyOfRange(allIds, startIndex, endIndex);

        java.util.List<String[]> data = new ArrayList<>();
        for (String id : subList) {
            String[] result = new String[showFields.length];

            Map<String, String> idData = showData(id);
            if (idData == null) {
                continue;
            }

            for (int i = 0; i < showFields.length; i++) {
                result[i] = idData.get(showFields[i]);
            }

            data.add(result);
        }
        return data;
    }

    @Override
    public void addData(Map<String, String> data) throws Exception {
        String file = data.get("file");
        File srcFile = new File(file);
        if (!srcFile.exists()) {
            getAppContext().getThreadLocalRequest().getResponse().setSuccess(false);
            return;
        }

        String fileName = srcFile.getName();

        String pathName;
        int typeIndex = fileName.indexOf(".");
        if (typeIndex > 0) {
            pathName = fileName.substring(0, typeIndex);
        } else {
            pathName = fileName;
        }
        File path = new File(new File(getAppContext().getAppDir(), FILE_BASEDIR), pathName);
        Files.createDirectories(path.toPath());
        Files.copy(srcFile.toPath(), new File(path, fileName).toPath());
    }

    @Override
    public void deleteData(String id) {
        File path = new File(getAppContext().getAppDir(), FILE_BASEDIR);
        if (path.exists()) {
            for (File file : path.listFiles()) {
                if (!file.isDirectory()) {
                    continue;// 这里是因为上传的时候，会以不带后缀的文件名作为目录名，所以需要过滤掉非目录
                }
                String fileName = file.getName();
                if (id.equals(fileName)) {
                    delDir(file);
                }
            }
        }
    }

    private static void delDir(File file) {
        if (file.isDirectory()) {
            File[] zFiles = file.listFiles();
            for (File file2 : zFiles) {
                delDir(file2);
            }
            file.delete();
        } else {
            file.delete();
        }
    }

    @Override
    public int totalSize(Map<String, String> query) {
        return allIds(query).length;
    }
}
