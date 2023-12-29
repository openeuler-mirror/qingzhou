package qingzhou.console.master.security;

import qingzhou.api.console.*;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;
import qingzhou.api.console.model.EditModel;
import qingzhou.api.console.model.ListModel;
import qingzhou.api.console.model.ShowModel;
import qingzhou.api.console.option.Option;
import qingzhou.api.console.option.OptionManager;
import qingzhou.console.ServerXml;
import qingzhou.console.master.MasterModelBase;
import qingzhou.console.util.Constants;
import qingzhou.console.util.StringUtil;
import qingzhou.console.util.XmlUtil;
import qingzhou.framework.impl.ServerUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Model(name = "userrole", icon = "check-sign",
        menuName = "Security", menuOrder = 3,
        nameI18n = {"角色分配", "en:User Role"},
        infoI18n = {"给管理员分配角色。", "en:Assign roles to administrators."})
public class UserRole extends MasterModelBase implements ListModel, EditModel {
    @ModelField(
            required = true, unique = true, showToList = true,
            refModel = User.class,
            allowRefModelDelete = true,
            type = FieldType.select, nameI18n = {"名称", "en:Name"},
            infoI18n = {"管理员的登录名。", "en:The administrator login name."})
    public String id;

    @ModelField(
            showToList = true,
            refModel = Role.class,
            type = FieldType.multiselect,
            nameI18n = {"角色", "en:Role"}, infoI18n = {"管理员的角色，给管理员分配不同的角色以控制访问不同的资源。",
            "en:Administrator roles, which assign different roles to administrators to control access to different resources."})
    public String roles = "";

    @ModelAction(name = ShowModel.ACTION_NAME_SHOW,
            icon = "info-sign", forwardToPage = "info",
            nameI18n = {"查看", "en:Show"},
            infoI18n = {"查看该组件的详细配置信息。", "en:View the detailed configuration information of the component."})
    public void show(Request request, Response response) throws Exception {
        if (!ServerXml.ConsoleRole.checkLoginUserIsManagerRole(request.getUserName(), true)) {
            response.setSuccess(false);
            response.setMsg(getConsoleContext().getI18N("user.not.permission"));
            return;
        }

        String id = request.getId();
        XmlUtil xmlUtils = new XmlUtil(ServerUtil.getServerXml());
        UserRole userRole = new UserRole();
        userRole.id = id;
        userRole.roles = xmlUtils.getSpecifiedAttribute(ServerXml.getTenantUserNodeExpression(ServerXml.getTenant(request.getUserName()), id), "roles");
        response.modelData().addDataObject(userRole, getConsoleContext());
    }

    @ModelAction(name = EditModel.ACTION_NAME_UPDATE,
            icon = "save",
            nameI18n = {"更新", "en:Update"},
            infoI18n = {"更新这个组件的配置信息，并尝试实时生效，对于不支持实时生效的更新则会在通知里添加一条提示重启TongWeb的消息。", "en:Update the configuration information of this component and try to take effect in real time, for updates that do not take effect in real time a message prompting a restart of TongWeb will be added to the notification."})
    public void update(Request request, Response response) throws Exception {
        writeForbid(request, response);
        if (!response.isSuccess()) {
            return;
        }

        String tenant = ServerXml.getTenant(request.getUserName());
        String userId = request.getId();
        XmlUtil xmlUtils = new XmlUtil(ServerUtil.getServerXml());
        String path = ServerXml.getTenantUserNodeExpression(tenant, userId);
        String oldRoles = xmlUtils.getSpecifiedAttribute(path, "roles");

        String roles = request.getParameter("roles");
        List<String> roleList = new ArrayList<>();
        if (StringUtil.notBlank(roles)) {
            for (String role : roles.split(Constants.DATA_SEPARATOR)) {
                if (role.endsWith(Constants.GROUP_SEPARATOR)) {
                    role = role.substring(0, role.length() - 1);
                }
                roleList.add(role);
            }
        }

        xmlUtils.setAttribute(path, "roles", StringUtil.join(roleList, Constants.DATA_SEPARATOR));
        xmlUtils.write();

        String newRoles = new XmlUtil(ServerUtil.getServerXml()).getSpecifiedAttribute(path, "roles");
        if (!isSameRoles(oldRoles, newRoles)) {
            for (String role : oldRoles.split(Constants.DATA_SEPARATOR)) {
                // TODO RoleCache.clearRoleCache(tenant, role);
            }
        }
    }

    @Override
    public void list(Request request, Response response) throws Exception {
        if (!ServerXml.ConsoleRole.checkLoginUserIsManagerRole(request.getUserName(), true)) {
            response.setSuccess(false);
            response.setMsg(getConsoleContext().getI18N("user.not.permission"));
            return;
        }
        ListModel.super.list(request, response);
    }

    @Override
    public List<Map<String, String>> listInternal(Request request, int start, int size) throws Exception {
        String expression = ServerXml.getTenantUserNodeExpression(ServerXml.getTenant(request.getUserName()), null);
        XmlUtil xmlUtils = new XmlUtil(ServerUtil.getServerXml());
        List<Map<String, String>> result = new ArrayList<>();
        start += 1; // 索引从1开始
        List<String> userList = xmlUtils.getAttributeList(expression + "/user[position() >= " + start + " and position() < " + (start + size) + "]/@" + ListModel.FIELD_NAME_ID);
        for (String userId : userList) {
            UserRole userRole = new UserRole();
            userRole.id = userId;
            userRole.roles = xmlUtils.getSpecifiedAttribute(ServerXml.getTenantUserNodeExpression(ServerXml.getTenant(request.getUserName()), userId), "roles");
            result.add(mapper(userRole));
        }

        return result;
    }

    @Override
    public int getTotalSize(Request request) throws Exception {
        XmlUtil xmlUtils = new XmlUtil(ServerUtil.getServerXml());
        String expression = ServerXml.getTenantUserNodeExpression(ServerXml.getTenant(request.getUserName()), null);
        return xmlUtils.getTotalSize(expression.substring(2) + "/user/@" + ListModel.FIELD_NAME_ID);
    }

    @Override
    public OptionManager fieldOptions(Request request, String fieldName) {
        if ("roles".equals(fieldName)) {
            List<Option> options = new ArrayList<>();
            try {
                XmlUtil xmlUtil = new XmlUtil(ServerUtil.getServerXml());
                List<String> roleIds = xmlUtil.getAttributeList(Role.getAllRoleIdExpression(request.getUserName()));
                for (String roleId : roleIds) {
                    options.add(Option.of(roleId));
                }
            } catch (Exception ignored) {
            }

            for (ServerXml.ConsoleRole.BuiltinRoleEnum xRole : ServerXml.ConsoleRole.BuiltinRoleEnum.values()) {
                options.add(Option.of(xRole.name(), new String[]{xRole.getInfo(), "en:" + xRole.name()}));
            }
            return () -> options;
        }

        return super.fieldOptions(request, fieldName);
    }

    private boolean isSameRoles(String oldRoles, String newRoles) {
        String[] news = trim(newRoles).split(Constants.DATA_SEPARATOR);
        String[] olds = trim(oldRoles).split(Constants.DATA_SEPARATOR);
        Arrays.sort(news);
        Arrays.sort(olds);
        return Arrays.equals(news, olds);
    }

    private String trim(String roles) {
        if (roles == null) {
            return "";
        }

        roles = roles.trim();
        if (roles.startsWith(Constants.DATA_SEPARATOR)) {
            roles = roles.substring(Constants.DATA_SEPARATOR.length());
        }
        if (roles.endsWith(Constants.DATA_SEPARATOR)) {
            roles = roles.substring(0, roles.length() - Constants.DATA_SEPARATOR.length());
        }
        return roles;
    }

    private void writeForbid(Request request, Response response) {
        // 校验用户是否为超级管理员，系统管理员
        String loginUser = request.getUserName();
        if (!ServerXml.ConsoleRole.checkLoginUserIsManagerRole(loginUser, true)) {
            response.setSuccess(false);
            response.setMsg(getConsoleContext().getI18N("user.not.permission"));
            return;
        }

        String id = request.getId();
        if (StringUtil.notBlank(id)) {
            if (ServerXml.ConsoleRole.systemXUsers().contains(id)) {
                response.setSuccess(false);
                response.setMsg(getConsoleContext().getI18N("permissions.cannot.users"));
            }
        }
    }
}
