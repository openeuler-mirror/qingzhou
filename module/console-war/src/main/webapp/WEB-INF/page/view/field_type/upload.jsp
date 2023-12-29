<%@ page pageEncoding="UTF-8" %>

<%
if(readonly == null) {
    return; // for 静态源码漏洞扫描
}
if(model == null) {
    return; // for 静态源码漏洞扫描
}

if (!"".equals(readonly)) {
    readonly = " onclick='return false;' readonly";
}
%>
<%
{
    %>
    <label class="radio-inline radio-label radio-anim">
        <input id="uploadRadio" type="radio" name="<%=fieldName%>"
               <%=ConsoleUtil.isDisableUpload()?"disabled":""%>
               value='<%=Constants.FILE_FROM_UPLOAD%>' <%=Objects.equals(fieldValue, Constants.FILE_FROM_UPLOAD) ? "checked" : ""%> <%=readonly%>>
        <i class="radio-i"></i> <%=I18n.getString(Constants.MASTER_APP_NAME, "fileFrom.upload")%>
    </label>
    <label class="radio-inline radio-label radio-anim">
        <input type="radio" name="<%=fieldName%>"
               value='<%=Constants.FILE_FROM_SERVER%>' <%=Objects.equals(fieldValue, Constants.FILE_FROM_SERVER) ? "checked" : ""%> <%=readonly%>>
        <i class="radio-i"></i> <%=I18n.getString(Constants.MASTER_APP_NAME, "fileFrom.server")%>
    </label>
    <%
}
%>
