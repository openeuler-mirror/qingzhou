<%@ page pageEncoding="UTF-8" %>

  <div class="block-bg">
    <%
      Map<String, Map<String, ModelFieldInfo>> formGroup = modelInfo.getFormGroupedField();
      Set<String> groups = formGroup.keySet();
      Iterator<String> iterator = groups.iterator();
      while (iterator.hasNext()) {
        Map<String, ModelFieldInfo> groupFieldMap = formGroup.get(iterator.next());
        Object[] overlap = ArrayUtils.overlap(groupFieldMap.keySet().toArray(), action.getShowFields());
        if (overlap == null || overlap.length == 0) {
          iterator.remove();
        }
      }
      long suffixId = System.currentTimeMillis();
      GroupInfo[] groupInfos = modelInfo.getGroupInfos();
      if (!groups.iterator().next().isEmpty()) {
    %>
    <ul class="nav nav-tabs">
      <%
        boolean isFirstGroup = true;
        for (String group : groups) {
          GroupInfo gInfo = null;
          if (groupInfos != null) {
            gInfo = Arrays.stream(groupInfos).filter(groupInfo -> groupInfo.getName().equals(group)).findAny().orElse(PageUtil.OTHER_GROUP);
          }
      %>
      <li <%=isFirstGroup ? "class='active'" : ""%>>
        <a data-tab href="#group-<%=group%>-<%=suffixId%>" data-toggle="tab"
           tabGroup="<%=group%>"><%=gInfo != null ? I18n.getStringI18n(gInfo.getI18n()) : group%>
        </a>
      </li>
      <%
          isFirstGroup = false;
        }
      %>
    </ul>
    <%
      }
    %>
    <div class="tab-content" style="padding-top: 12px; padding-bottom: 12px;">
      <%
        boolean isFirstGroup = true;
        for (String group : groups) {
      %>
      <div class="tab-pane <%=isFirstGroup?"active":""%>"
           id="group-<%=group%>-<%=suffixId%>"
           tabGroup="<%=group%>">
        <%
          isFirstGroup = false;
          Map<String, ModelFieldInfo> groupFieldMap = formGroup.get(group);
          for (Map.Entry<String, ModelFieldInfo> e : groupFieldMap.entrySet()) {
            String fieldName = e.getKey();
            if (!ArrayUtils.contains(action.getShowFields(), fieldName)) {
              continue;
            }

            ModelFieldInfo modelField = e.getValue();

            String readonly = "";
            String fieldValue = modelData.get(fieldName);
            if (fieldValue == null) {
              fieldValue = "";
            }
            java.util.List<String> fieldValues = Arrays.asList(fieldValue.split(modelField.getSeparator()));
        %>
        <div class="form-group" id="form-item-<%=fieldName%>">
          <label for="<%=fieldName%>" class="col-sm-4">
            <%=I18n.getModelI18n(qzApp, "model.field." + qzModel + "." + fieldName)%>
            <%
              String fieldInfo = I18n.getModelI18n(qzApp, "model.field.info." + qzModel + "." + fieldName);
              if (fieldInfo != null) {
                // 注意：下面这个 title=xxxx 必须使用单引号，因为 Model 的注解里面用了双引号，会导致显示内容被截断!
                fieldInfo = "<span class='tooltips' data-tip='" + fieldInfo + "' data-tip-arrow='bottom-right'><i class='icon icon-question-sign'></i></span>";
              } else {
                fieldInfo = "";
              }
            %>
            <%=fieldInfo%>
          </label>
          <div class="col-sm-5">
            <%
              if (!readonly.isEmpty()) {
                if (FieldType.textarea.name().equals(modelField.getType())) {
            %>
            <textarea rows="3" disabled="disabled" name="<%=fieldName%>" class="form-control"
                      readonly="readonly"><%=fieldValue%></textarea>
            <%
            } else {
            %>
            <input type="text" name="<%=fieldName%>" value='<%=fieldValue%>'
                   class="form-control" readonly="readonly">
            <%
              }
            } else {
              FieldType fieldType = FieldType.valueOf(modelField.getType());
              switch (fieldType) {
                case text:
            %>
            <input type="text" name="<%=fieldName%>" value='<%=fieldValue%>'
                   class="form-control">
            <%
                break;
              case number:
            %>
            <input type="number" min="<%=modelField.getMin()%>"
                   max="<%=(modelField.isPort()?"65535":modelField.getMax())%>"
                   name="<%=fieldName%>" value='<%=fieldValue%>' class="form-control">
            <%
                break;
              case decimal:%>
            <%@ include file="../view/field_type/decimal.jsp" %>
            <%
                break;
              case password:
            %>
            <input type="password" name="<%=fieldName%>" value='<%=fieldValue%>' data-type="password"
                   class="form-control">
            <label password_label_right="<%=fieldName%>_eye" class="input-control-icon-right"
                   style="margin-right: 10px; cursor: pointer;"><i class="icon icon-eye-close"></i></label>
            <%
                break;
              case textarea:
            %>
            <textarea name="<%=fieldName%>" value='<%=fieldValue%>' class="form-control"
                      rows="3"><%=fieldValue%></textarea>
            <%
                break;
              case markdown:
            %>
            <div class="markedview"></div>
            <textarea name="<%=fieldName%>" class="markedviewText" rows="3"><%=fieldValue%></textarea>
            <%
                break;
              case radio:%>
            <%@ include file="../view/field_type/radio.jsp" %>
            <%
                break;
              case bool:
            %>
            <%@ include file="../view/field_type/bool.jsp" %>
            <%
                break;
              case select:
            %>
            <%@ include file="../view/field_type/select.jsp" %>
            <%
                break;
              case multiselect:
            %>
            <%@ include file="../view/field_type/multiselect.jsp" %>
            <%
                break;
              case checkbox:
            %>
            <%@ include file="../view/field_type/checkbox.jsp" %>
            <%
                break;
              case sortablecheckbox:
            %>
            <%@ include file="../view/field_type/sortablecheckbox.jsp" %>
            <%
                break;
              case file:
            %>
            <%@ include file="../view/field_type/file.jsp" %>
            <%
                break;
              case sortable:
            %>
            <%@ include file="../view/field_type/sortable.jsp" %>
            <%
                break;
              case kv:
            %>
            <%@ include file="../view/field_type/kv.jsp" %>
            <%
                break;
              case datetime:
            %>
            <%@ include file="../view/field_type/datetime.jsp" %>
            <%
                    break;
                  default:
                    throw new IllegalStateException(modelField.getType() + ".jsp not found.");
                }
              }
            %>
            <label class="tw-error-info"></label>
          </div>
        </div>
        <%
          }
        %>
      </div>
      <%
        }
      %>
    </div>
  </div>
