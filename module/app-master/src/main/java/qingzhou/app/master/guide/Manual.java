package qingzhou.app.master.guide;

import qingzhou.app.master.ReadOnlyDataStore;
import qingzhou.framework.api.*;

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
            nameI18n = {"索引", "en:Index"},
            infoI18n = {"此手册的索引。", "en:The index of this manual."})
    public String id;

    @ModelField(
            showToList = true,
            nameI18n = {"名称", "en:Name"},
            infoI18n = {"此手册的名称。", "en:The name of the manual."})
    public String name;

    @ModelField(nameI18n = {"内容", "en:Content"},
            type = FieldType.markdown,
            infoI18n = {"此手册的内容。", "en:The content of the manual."})
    public String info;

    @Override
    public void init() {
        ConsoleContext context = getAppContext().getConsoleContext();

        context.addI18N("manual.name.1", new String[]{"平台概述", "en:Overview of Qingzhou platform"});
        context.addI18N("manual.info.1", new String[]{"# 平台概述\n" +
                "## 平台简介\n" +
                "轻舟是一款轻量级的软件开发平台，主要用于Web应用类型软件的开发。轻舟实现了自动化的前端网页生成、后端参数校验，并内置了用户管理、权限控制、日志接口、系统升级等开箱即用的通用能力，使得开发者只需专注业务本身代码的编写即可，从而提升Web应用的开发效率。与其它类似软件相比，轻舟的特色在于，基于它所开发的Web应用是可插拔的，即在轻舟平台上可按需安装多套相同或不同类型业务系统的Web应用，并可在不需要的时候将其卸载，这正是轻舟的平台性能力所在。轻舟的愿景是为业务系统提供通用的、安全的、简单快速的，开发与管理一体化支撑能力。\n" +
                "### 典型用途\n" +
                "1. 开发一般产品的Web管控台\n" +
                "2. 为分布式软件系统做集中式管理\n"
                , "en:# Overview of Qingzhou platform\n" +
                "## Introduction to the platform\n" +
                "Qingzhou is a lightweight software development platform, mainly used for the development of web application software. Qingzhou realizes automatic front-end web page generation, back-end parameter verification, and built-in user management, permission control, log interface, system upgrade and other out-of-the-box general capabilities, so that developers only need to focus on the writing of business code, so as to improve the development efficiency of Web applications. Compared with other similar software, the feature of Qingzhou is that the Web applications developed based on it are pluggable, that is, multiple sets of Web applications of the same or different types of business systems can be installed on the Qingzhou platform on demand, and they can be uninstalled when they are not needed, which is where the platform capabilities of Qingzhou lie. Qingzhou's vision is to provide general, safe, simple and fast, integrated development and management support capabilities for business systems.\n" +
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
}
