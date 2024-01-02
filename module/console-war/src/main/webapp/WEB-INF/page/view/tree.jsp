<%@ page pageEncoding="UTF-8" %>
<%@ include file="../fragment/head.jsp" %>

<%
    List<TreeModel> treeModels = new ArrayList<>();
    for (ModelBase obj : ConsoleUtil.convertMapToModelBase(qzRequest, qzResponse)) {
        treeModels.add((TreeModel) obj);
    }
    long nowId = System.currentTimeMillis();
%>

<div class="bodyDiv">
    <%-- 面包屑分级导航 --%>
    <%@ include file="../fragment/breadcrumb.jsp" %>

    <div jndiContainer="true" class="row" style="margin-left: -14px; margin-right: -14px;">
        <div class="col-xs-3" style="padding-left: 16px; padding-right: 0px;">
            <ul id="jndiNav" jndiNav="true" class="nav nav-tabs nav-stacked"
                style="border: 0px; overflow-x:hidden; overflow-y:auto; min-height: 450px; padding-top:4px;padding-bottom:16px;">
                <%
                    int tabIndex = 0;
                    for (Map<String, String> treeName : qzResponse.getDataList()) {
                        tabIndex++;
                %>
                <li<%=(tabIndex == 1 ? " class=\"active\"" : "")%>>
                    <a href="#<%=(nowId + "_" + tabIndex)%>" data-tab>
                        <span style="font-size:15px;"><%=treeName%></span>
                    </a>
                </li>
                <%}%>
            </ul>
        </div>
        <div class="col-xs-9" style="padding-left: 16px; padding-right: 16px;">
            <div class="tab-content block-bg" style="padding-left:0px; padding-right:0px;">
                <%
                    int paneIndex = 0;
                    for (TreeModel treeModel : treeModels) {
                        paneIndex++;
                        String treeName = treeModel.treeName();
                %>
                <ul id="<%=(nowId + "_" + paneIndex)%>" treeView="true"
                    class="tab-pane<%=(paneIndex == 1 ? " active" : "")%> tree-lines"
                    style="list-style:none; padding-left:8px; padding-right:0px; padding-top: 0px;">
                    <textarea tree-data="<%=treeName + paneIndex%>" index="<%=paneIndex%>" rows="3" disabled="disabled"
                              style="display:none;">
                        <%=JsonView.convertJson(treeModel)%>
                    </textarea>
                </ul>
                <%}%>
            </div>
        </div>
        <textarea name="treeKeys" rows="3" disabled="disabled" style="display: none;">
        <%
            StringBuilder keysBuilder = new StringBuilder();
            keysBuilder.append("{");
            if (!treeModels.isEmpty()) {
                TreeModel.TreeNode treeNode = treeModels.get(0).treeData().get(0);
                for (Map.Entry<String, String> e : treeNode.getInfo().entrySet()) {
                    String k = e.getKey();
                    String v = I18n.getString("model.field." + actionContext.getModelName() + "." + k);
                    keysBuilder.append("\"").append(k).append("\":\"").append(v).append("\",");
                }
            }
            if (keysBuilder.indexOf(",") > 0) {
                keysBuilder.deleteCharAt(keysBuilder.lastIndexOf(","));
            }
            keysBuilder.append("}");
            out.print(keysBuilder.toString());
        %>
        </textarea>
    </div>
</div>
