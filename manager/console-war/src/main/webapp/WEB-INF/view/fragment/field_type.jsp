<%@ page pageEncoding="UTF-8" %>

<%
    echoGroup = "";
    if (modelField.getEchoGroup().length > 0) {
        String echoGroups = String.join(",", modelField.getEchoGroup());
        echoGroup = "echoGroup='" + echoGroups + "'";
    }

    isDisabled = fieldName.equals(idField) && isEdit;
    if (!isDisabled) {
        isDisabled = modelField.isPlainText();
    }
    if (isDisabled) {
        if (fieldValue.isEmpty()) {
            fieldValue = modelField.getDefaultValue();
        }
%>
<input type="text" disabled="disabled" name="<%=fieldName%>"
       placeholder="<%=modelField.getPlaceholder()%>"
       value='<%=fieldValue%>' <%=echoGroup%>
       class="form-control"/>
<%
} else if (modelField.isReadonly()) {
%>
<input type="text" readonly name="<%=fieldName%>"
       placeholder="<%=modelField.getPlaceholder()%>"
       style="cursor: not-allowed;border: 1px solid #DCDCDC;background-color: #e5e5e5;"
       value='<%=fieldValue%>' <%=echoGroup%>
       class="form-control"/>
<%
} else {

    InputType inputType = modelField.getInputType();
    switch (inputType) {
        case text:
%>
<input type="text"
       placeholder="<%=modelField.getPlaceholder()%>"
       name="<%=fieldName%>" value='<%=fieldValue%>' <%=echoGroup%>
       class="form-control">
<%
        break;
    case number:
%>
<input type="number" min="<%=modelField.getMin()%>"
       placeholder="<%=modelField.getPlaceholder()%>"
       max="<%=(modelField.isPort()?"65535":modelField.getMax())%>"
       name="<%=fieldName%>" value='<%=fieldValue%>' <%=echoGroup%> class="form-control">
<%
        break;
    case decimal:%>
<%@ include file="field_type/decimal.jsp" %>
<%
        break;
    case password:
        passwordFields.add(fieldName);
%>
<input type="password"
       placeholder="<%=modelField.getPlaceholder()%>"
       name="<%=fieldName%>" value='<%=fieldValue%>' data-type="password"
       class="form-control">
<label password_label_right="<%=fieldName%>_eye" class="input-control-icon-right"
       style="margin-right: 10px; cursor: pointer;"><i class="icon icon-eye-close"></i></label>
<%
        break;
    case textarea:
%>
<textarea name="<%=fieldName%>"
          placeholder="<%=modelField.getPlaceholder()%>"
          value='<%=fieldValue%>' <%=echoGroup%> class="form-control"
          rows="3"><%=fieldValue%></textarea>
<%
        break;
    case markdown:
%>
<div class="markedview"></div>
<textarea name="<%=fieldName%>"
          placeholder="<%=modelField.getPlaceholder()%>"
          class="markedviewText" rows="3"><%=fieldValue%></textarea>
<%
        break;
    case radio:%>
<%@ include file="field_type/radio.jsp" %>
<%
        break;
    case bool:
%>
<%@ include file="field_type/bool.jsp" %>
<%
        break;
    case select:
%>
<%@ include file="field_type/select.jsp" %>
<%
        break;
    case multiselect:
%>
<%@ include file="field_type/multiselect.jsp" %>
<%
        break;
    case checkbox:
%>
<%@ include file="field_type/checkbox.jsp" %>
<%
        break;
    case sortable_checkbox:
%>
<%@ include file="field_type/sortablecheckbox.jsp" %>
<%
        break;
    case file:
%>
<%@ include file="field_type/file.jsp" %>
<%
        break;
    case sortable:
%>
<%@ include file="field_type/sortable.jsp" %>
<%
        break;
    case kv:
%>
<%@ include file="field_type/kv.jsp" %>
<%
        break;
    case datetime:
%>
<%@ include file="field_type/datetime.jsp" %>
<%
        break;
    case range_datetime:
%>
<%@ include file="field_type/range_datetime.jsp" %>
<%
        break;
    case combine:
%>
<%@ include file="field_type/combine.jsp" %>
<%
            default:
                throw new IllegalStateException(modelField.getInputType().name() + ".jsp not found.");
        }
    }
%>