package qingzhou.app.model;

import qingzhou.api.*;
import qingzhou.api.type.Listable;

import java.util.ArrayList;
import java.util.List;

@Model(name = "Grid", icon = "table",
        nameI18n = {"Grid模块", "en:Grid Test"},
        infoI18n = {"测试增删改查相关功能", "en:Test the functions related to adding, deleting, modifying, and querying"}
)
public class Grid extends ModelBase implements Listable {

    @ModelAction(name = Listable.ACTION_NAME_LIST,
            icon = "table", forwardToPage = "grid",
            nameI18n = {"网格", "en:grid"},
            infoI18n = {"grid", "en:grid"})
    public void list(Request request, Response response) throws Exception {
        List<App> list = new ArrayList<>();
        String[] apps = {"Apache Tomcat Manager", "Nacos Manager", "OpenSearchMgr", "RocketMQMgr", "SpringBootAdminExt", "RedisAdmin", "XXXMgr", "ABCmanager", "xxAbc", "twManager", "Test", "qzMgr"};
        String detail = "这是应用的详细信息，用于对应用的功能特点及相关要求和使用等进行描述。";
        for (int i = 0; i < 55; i++) {
            String version = "1.0.0" + (i % 3 == 0 ? "-beta1" : "");
            list.add(new App(String.valueOf(1000 + i), (i > 11 ? ("随机" + i) : apps[i]), apps[i % 12] + ".svg", version, detail, i % 5 == 0 ? "0.9.8" : (i == 4 ? version : "")));
        }
        int pageNum = getParamToInt(request, Listable.PARAMETER_PAGE_NUM, 1);
        List<App> pageList = list.subList(pageSize() * (pageNum - 1), pageSize() * pageNum > (list.size() - 1) ? (list.size() - 1) : pageSize() * pageNum);
        for (int i = 0; i < pageList.size(); i++) {
            response.addModelData(pageList.get(i));
        }
        response.setPageNum(pageNum);
        response.setPageSize(pageSize());
        response.setTotalSize(list.size());
    }

    public int getParamToInt(Request request, String parameterName, int defaultValue) {
        String parameter = request.getParameter(parameterName);
        return parameter == null ? defaultValue : Integer.parseInt(parameter);
    }
}
