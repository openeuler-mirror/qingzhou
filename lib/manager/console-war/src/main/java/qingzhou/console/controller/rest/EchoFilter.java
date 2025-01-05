package qingzhou.console.controller.rest;

import qingzhou.api.MsgLevel;
import qingzhou.api.type.Echo;
import qingzhou.console.controller.SystemController;
import qingzhou.core.deployer.ActionInvoker;
import qingzhou.core.deployer.RequestImpl;
import qingzhou.core.deployer.ResponseImpl;
import qingzhou.engine.util.pattern.Filter;

public class EchoFilter implements Filter<RestContext> {
    @Override
    public boolean doFilter(RestContext context) {
        RequestImpl request = context.request;
        if (request.getAction().equals(Echo.ACTION_ECHO)) {
            ResponseImpl response = (ResponseImpl) SystemController.getService(ActionInvoker.class).invokeAny(request);
            request.setResponse(response); // 用远程的响应，替换掉本地无数据的响应对象，如果没有执行远程，那么它俩应是等效的
            if (response.getMsgLevel() == null) {
                response.setMsgLevel(response.isSuccess() ? MsgLevel.INFO : MsgLevel.ERROR);
            }
            return false;
        }
        return true;
    }
}