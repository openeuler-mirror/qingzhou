<%@ page pageEncoding="UTF-8" %>

<%-- 面包屑分级导航 --%>
<ol class="breadcrumb" style="font-size: 15px; margin-bottom: 0px; min-width: 300px; padding: 10px 5px !important;">
    <li class="active" style="margin-left:-5px;">
        <div class="model-info">
            <span><%=I18n.getString(qzRequest, "model." + qzRequest.getModelName())%></span>
            <span class="tooltips" data-tip='<%=I18n.getString(qzRequest, "model.info." + qzRequest.getModelName())%>' data-tip-arrow="bottom-right" style="line-height:25px;">
				<i class="icon icon-question-sign"></i>
			</span>
        </div>
    </li>
    <%
        if (!Objects.equals(I18n.getString(qzRequest, "model.action." + qzRequest.getModelName() + "." + qzRequest.getModelName()), I18n.getString(qzRequest, "model." + qzRequest.getModelName()))) {
            if (StringUtil.notBlank(I18n.getString(qzRequest, "model.action." + qzRequest.getModelName() + "." + qzRequest.getActionName()))) {
                %>
                <li class="active">
                    <%=I18n.getString(qzRequest, "model.action." + qzRequest.getModelName() + "." + qzRequest.getActionName())%>
                </li>
                <%
            }
        }
    %>

    <script type="text/javascript">
        function favoirtes(url) {
            $.ajax({
                url: url,
                type: "post",
                async: false,
                success: function (data) {
                    if (data.success) {
                        showSuccess(data.message, function () {
                            window.location.href = window.location.href;
                        });
                    } else {
                        showError(data.message);
                    }
                }
            });
        }
    </script>
    <%
        // 收藏
        if (!Constants.MODEL_NAME_home.equals(qzRequest.getModelName())
                && !Constants.MODEL_NAME_index.equals(qzRequest.getModelName())
                && StringUtil.isBlank(qzRequest.getId())
                && (AccessControl.canAccess(qzRequest.getTargetType(), qzRequest.getTargetName(),Constants.MODEL_NAME_favorites + "/" + ConsoleUtil.ACTION_NAME_addfavorite, LoginManager.getLoginUser(session)))
                && (AccessControl.canAccess(qzRequest.getTargetType(), qzRequest.getTargetName(),Constants.MODEL_NAME_favorites + "/" + ConsoleUtil.ACTION_NAME_cancelfavorites, LoginManager.getLoginUser(session)))
        ) {
            if (ServerXml.isMyFavorites(currentUser, qzRequest.getTargetName(), qzRequest.getModelName(), qzRequest.getActionName())) {
                %>
                <a class="cancelfavorites" href="javascript:void(0);" style="margin-left: 10px;"
                   title='<%=I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "model.action." + Constants.MODEL_NAME_favorites + "." + ConsoleUtil.ACTION_NAME_cancelfavorites)%>'
                   onclick='favoirtes("<%=ConsoleUtil.encodeURL(request,response,ViewManager.jsonView+"/" + Constants.MODEL_NAME_instance+"/"+ Constants.QINGZHOU_MASTER_APP_NAME +"/"+Constants.MODEL_NAME_favorites+"/"+ConsoleUtil.ACTION_NAME_cancelfavorites+"?favorite="+qzRequest.getTargetType()+"/"+qzRequest.getTargetName()+"/"+qzRequest.getModelName()+"/"+qzRequest.getActionName())%>")'>
                    <i class="icon icon-star"></i>
                </a>
                <%
            } else {
                %>
                <a class="addfavotites" href="javascript:void(0);" style="margin-left: 10px;"
                   title='<%=I18n.getString(Constants.QINGZHOU_MASTER_APP_NAME, "model.action." + Constants.MODEL_NAME_favorites + "." + ConsoleUtil.ACTION_NAME_addfavorite)%>'
                   onclick='favoirtes("<%=ConsoleUtil.encodeURL(request,response,ViewManager.jsonView+"/" + Constants.MODEL_NAME_instance+"/"+ Constants.QINGZHOU_MASTER_APP_NAME +"/"+Constants.MODEL_NAME_favorites+"/"+ConsoleUtil.ACTION_NAME_addfavorite+"?favorite="+qzRequest.getTargetType()+"/"+qzRequest.getTargetName()+"/"+qzRequest.getModelName()+"/"+qzRequest.getActionName())%>")'>
                    <i class="icon icon-star-empty"></i>
                </a>
                <%
            }
        }
    %>
</ol>
