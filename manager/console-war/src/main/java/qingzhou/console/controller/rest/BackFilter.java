package qingzhou.console.controller.rest;

import qingzhou.console.view.type.HtmlView;
import qingzhou.deployer.RequestImpl;
import qingzhou.deployer.ResponseImpl;
import qingzhou.engine.util.pattern.Filter;

import javax.servlet.http.HttpSession;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class BackFilter implements Filter<RestContext> {
    public static final String BACK_URI = "back";
    public static final String HISTORY_KEY = "history";

    @Override
    public boolean doFilter(RestContext context) throws Exception {
        RequestImpl request = context.request;
        if (request.getAction().equals(BACK_URI)) {
            HttpSession session = context.req.getSession();
            Map<String,Deque<RequestImpl>> historyMap = (Map<String, Deque<RequestImpl>>) session.getAttribute(HISTORY_KEY);
            if (historyMap != null) {
                Deque<RequestImpl> appHistory = historyMap.get(request.getApp());
                if (appHistory != null && !appHistory.isEmpty()) {
                    appHistory.removeFirst(); // 当前页
                    RequestImpl historyRequest = appHistory.peekFirst();
                    if (historyRequest != null) {
                        historyRequest.setResponse(new ResponseImpl());
                        context.request = historyRequest;
                        return true;
                    }
                }
            }
            return false;
        }
        if (request.getView().equals(HtmlView.FLAG)) {
            HttpSession session = context.req.getSession();
            Map<String,Deque<RequestImpl>> historyMap = (Map<String, Deque<RequestImpl>>) session.getAttribute(HISTORY_KEY);
            if (historyMap == null) {
                historyMap = new HashMap<>();
                session.setAttribute("history", historyMap);
            }
            Deque<RequestImpl> appHistory = historyMap.computeIfAbsent(request.getApp(), k -> new ArrayDeque<>(10));
            if (appHistory.size() == 10) {
                appHistory.removeLast();
            }
            appHistory.addFirst(request);
        }
        return true;
    }
}
