package qingzhou.console.controller.rest;

import qingzhou.api.MsgLevel;
import qingzhou.api.Response;
import qingzhou.api.type.Echo;
import qingzhou.console.controller.SystemController;
import qingzhou.deployer.ActionInvoker;
import qingzhou.deployer.RequestImpl;
import qingzhou.engine.util.pattern.Filter;

public class EchoFilter implements Filter<RestContext> {
    @Override
    public boolean doFilter(RestContext context) {
        RequestImpl request = context.request;
        if (request.getAction().equals(Echo.ACTION_ECHO)) {
            Response response = SystemController.getService(ActionInvoker.class).invokeSingle(request);
            request.setResponse(response); // 用远程的响应，替换掉本地无数据的响应对象，如果没有执行远程，那么它俩应是等效的
            if (response.getMsgType() == null) {
                response.setMsgType(response.isSuccess() ? MsgLevel.info : MsgLevel.error);
            }
            return false;
        }
        return true;
    }
}