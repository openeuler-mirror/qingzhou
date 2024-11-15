package qingzhou.console.controller.rest;

import qingzhou.api.Request;
import qingzhou.console.view.type.HtmlView;
import qingzhou.deployer.RequestImpl;
import qingzhou.engine.util.pattern.Filter;

import javax.servlet.http.HttpSession;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

//            new BackFilter(), // 因前端 子菜单跳转 等功能支持，暂无法结合后端历史请求处理，会导致前端控制不到显示区域和菜单定位等问题
public class BackFilter implements Filter<RestContext> {
    public static final String BACK_URI = "qz_back_action"; // 避免和应用的 action 名字冲突
    private static final String BACK_KEY = "qz_back_cache";

    public static String getBackModel(HttpSession session, Request qzRequest) {
        Map<String, Deque<RequestImpl>> historyMap = (Map<String, Deque<RequestImpl>>) session.getAttribute(BACK_KEY);
        if (historyMap != null) {
            Deque<RequestImpl> appHistory = historyMap.computeIfAbsent(qzRequest.getApp(), k -> new ArrayDeque<>(10));
            if (appHistory.size() > 1) {
                RequestImpl currentReq = appHistory.removeFirst();  // 当前页
                RequestImpl lastReq = appHistory.peekFirst();
                appHistory.addFirst(currentReq);
                if (lastReq != null) {
                    return lastReq.getModel();
                }
            }
        }
        return null;
    }

    @Override
    public boolean doFilter(RestContext context) throws Exception {
        RequestImpl request = context.request;
        if (request.getAction().equals(BACK_URI)) {
            HttpSession session = context.req.getSession();
            Map<String, Deque<RequestImpl>> historyMap = (Map<String, Deque<RequestImpl>>) session.getAttribute(BACK_KEY);
            if (historyMap != null) {
                Deque<RequestImpl> appHistory = historyMap.get(request.getApp());
                if (appHistory != null && !appHistory.isEmpty()) {
                    appHistory.removeFirst(); // 当前页
                    RequestImpl historyRequest = appHistory.peekFirst();
                    if (historyRequest != null) {
                        context.request = historyRequest;
                        return true;
                    }
                }
            }
        }

        if (request.getView().equals(HtmlView.FLAG)) {
            HttpSession session = context.req.getSession();
            Map<String, Deque<RequestImpl>> historyMap = (Map<String, Deque<RequestImpl>>) session.getAttribute(BACK_KEY);
            if (historyMap == null) {
                historyMap = new HashMap<>();
                session.setAttribute(BACK_KEY, historyMap);
            }
            Deque<RequestImpl> appHistory = historyMap.computeIfAbsent(request.getApp(), k -> new ArrayDeque<>(10));
            if (appHistory.size() == 10) {
                appHistory.removeLast();
            }
            RequestImpl backRequest = new RequestImpl(request);
            backRequest.setCachedModelInfo(request.getCachedModelInfo());
            for (Map.Entry<String, String> e : request.getParameters().entrySet()) {
                backRequest.getParameters().put(e.getKey(), e.getValue());
            }
            appHistory.addFirst(backRequest);
        }

        return true;
    }
}
