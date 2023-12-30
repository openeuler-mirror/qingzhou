package qingzhou.master.service;

import qingzhou.framework.api.Model;
import qingzhou.framework.api.ModelField;
import qingzhou.framework.api.AddModel;
import qingzhou.master.MasterModelBase;

@Model(name = "cluster", icon = "sitemap",
        menuName = "Service", menuOrder = 2,
        nameI18n = {"集群", "en:Cluster"},
        infoI18n = {"集群是一组具有相同配置的实例。",
                "en:A cluster is a group of instances with the same configuration."})
public class Cluster extends MasterModelBase implements AddModel {
    @ModelField(
            required = true, unique = true, showToList = true,
            nameI18n = {"ID", "en:ID"},
            infoI18n = {"ID。", "en:ID."})
    public String id;

    @ModelField(
            required = true, showToList = true,
            nameI18n = {"名称", "en:Name"},
            infoI18n = {"唯一标识。", "en:Unique identifier."})
    public String name;

    @ModelField(
            required = true, showToList = true,
            nameI18n = {"类型", "en:Type"},
            infoI18n = {"类型。", "en:Unique identifier."})
    public String type;
}
