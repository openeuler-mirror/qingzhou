<%@ page pageEncoding="UTF-8" %>

<%
    if (readonly == null) {
        return; // for 静态源码漏洞扫描
    }

    String tempId = java.util.UUID.randomUUID().toString().replaceAll("-", "");
    String tempEleId = java.util.UUID.randomUUID().toString().replaceAll("-", "");
    if (!"".equals(readonly)) {
        readonly = " onclick='return false;' readonly";
    }
%>
<div id="uploader" class="uploader">
    <a href="javascript:;" onclick="$('#<%=tempId%>').click();" class="btn uploader-btn-browse"
       style="width: 108px; height: 32px; border-radius: 1px; background: #FFFFFF;">
        <i class="icon icon-upload-alt"></i>
        <span style="padding-left: 6px; font-family: PingFangSC-Regular; font-size: 14px; color: #262626; line-height: 20px;"><%=I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "page.selectfile")%></span>
    </a>
    <%--
    <!-- <a href="javascript:;"
       onclick="$('#<%=tempId%>').val('');$('#<%=tempEleId%>').html('');"
       style="margin-left:10px; color: #000;">
        <i class="icon icon-undo"></i>
    </a> --> --%>
    <div id="<%=tempEleId%>"></div>
    <input id="<%=tempId%>" <%=readonly%> type="file" name="<%=fieldName%>" style="display:none;"
           onchange="$('#<%=tempEleId%>').html(this.value.indexOf('fakepath') > 0 ? this.value.substring(this.value.indexOf('fakepath') + 'fakepath'.length + 1) : this.value);">
</div>