package qingzhou.app.master.guide;

import qingzhou.app.master.ReadOnlyDataStore;
import qingzhou.framework.api.ConsoleContext;
import qingzhou.framework.api.DataStore;
import qingzhou.framework.api.FieldType;
import qingzhou.framework.api.ListModel;
import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelAction;
import qingzhou.framework.api.ModelBase;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.Request;
import qingzhou.framework.api.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Model(name = "manual", icon = "book",
        menuName = "Guide", menuOrder = 3,
        nameI18n = {"开发手册", "en:DevManual"},
        infoI18n = {"基于轻舟进行应用开发的具体操作说明。", "en:Specific operating instructions for application development based on Qingzhou."})
public class Manual extends ModelBase implements ListModel {
    @ModelField(
            showToList = true,
            nameI18n = {"操作项", "en:Item"},
            infoI18n = {"操作说明的名称。", "en:The name of the operation instructions."})
    public String id;

    @ModelField(
            showToList = true,
            nameI18n = {"操作项", "en:Item"},
            infoI18n = {"操作说明的名称。", "en:The name of the operation instructions."})
    public String name;

    @ModelField(nameI18n = {"操作说明", "en:Instructions"},
            type = FieldType.markdown,// todo: 支持在 info.jsp 里面展示 markdown 格式
            infoI18n = {"操作说明的名称。", "en:The name of the operation instructions."})
    public String info;

    @Override
    public void init() {
        ConsoleContext context = getAppContext().getConsoleContext();

        context.addI18N("manual.name.1", new String[]{"轻舟Web管理软件开发平台概述", "en:Overview of Qingzhou web management software development platform"});
        context.addI18N("manual.info.1", new String[]{"# 轻舟Web管理软件开发平台概述\n" +
                "## 平台简介\n" +
                "轻舟是一个轻量、快速的软件开发平台，可用于Web管理类软件的开发。基于轻舟，开发者可专注于核心业务代码的编写，而无需关心视图层逻辑的实现，从而提高开发效率。轻舟提供了表单页、列表页、监视页、文件下载页等网页显示模板，用以满足不同类型的UI需求，同时内置了与Web管理相关的认证授权、远程部署、REST接口、参数检查等能力，力求为业务系统提供简单、快速且功能完备的开发与管理一体化支撑能力。\n" +
                "### 典型用途\n" +
                "1. 开发一般产品的Web管控台\n" +
                "2. 为分布式软件系统做集中式管理\n"
                , "en:# Overview of Qingzhou web management software development platform\n" +
                "## Introduction to the platform\n" +
                "Qingzhou is a lightweight and fast software development platform, which can be used for the development of web management software. Based on Qingzhou, developers can focus on writing core business code without worrying about the implementation of view layer logic, thereby improving development efficiency. Qingzhou provides web display templates such as form pages, list pages, monitoring pages, and file download pages to meet different types of UI needs, and has built-in capabilities such as authentication and authorization, remote deployment, REST interface, and parameter check related to Web management, striving to provide simple, fast and complete integrated support capabilities for development and management of business systems.\n" +
                "### Typical uses\n" +
                "1. Develop a web console for general products\n" +
                "2. Centralized management of distributed software systems\n"
        });

        // todo 补充内容包括：应用开发、REST接口、命令行接口、IDE集成等
        context.addI18N("manual.name.2", new String[]{"应用开发指南", "en:App Development Guide"});
        context.addI18N("manual.info.2", new String[]{"# 应用开发指南\n" +
                "## REST接口\n" +
                "## 命令行接口\n"
                , "en:# App Development Guide\n" +
                "## REST interface\n" +
                "## Command-line interface\n"
        });
    }

    @Override
    public DataStore getDataStore() {
        return new ReadOnlyDataStore(type -> {
            ConsoleContext context = getAppContext().getConsoleContext();
            List<Map<String, String>> data = new ArrayList<>();
            for (int i = 1; i <= 3; i++) { // 需要和 init 里面的 i18n 的序号保持一致
                Map<String, String> model = new HashMap<>();
                model.put("id", "manual-" + i);
                model.put("name", context.getI18N(getI18nLang(), "manual.name." + i));
                model.put("info", context.getI18N(getI18nLang(), "manual.info." + i));
                data.add(model);
            }
            return data;
        });
    }

    @Override
    @ModelAction(name = ACTION_NAME_SHOW,
            showToList = true,
            icon = "info-sign", forwardToPage = "show",
            nameI18n = {"查看", "en:Show"},
            infoI18n = {"查看该组件的相关信息。", "en:View the information of this model."})
    public void show(Request request, Response response) throws Exception {
        super.show(request, response);
    }
}
