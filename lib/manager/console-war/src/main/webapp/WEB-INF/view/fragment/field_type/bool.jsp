<%@ page pageEncoding="UTF-8" %>

<div class="switch-btn">
    <div class="switchedge <%="true".equals(fieldValue) ? "switch-bg":""%>">
        <div class="circle <%="true".equals(fieldValue) ? "switch-right":""%>"></div>
    </div>
    <input type="hidden" name="<%=fieldName%>" value='<%=fieldValue%>' <%=echoGroup%>>
</div>
