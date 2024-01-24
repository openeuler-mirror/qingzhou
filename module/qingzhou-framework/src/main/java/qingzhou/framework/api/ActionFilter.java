package qingzhou.framework.api;

public interface ActionFilter {
    String doFilter(Request request, Response response, AppContext appContext) throws Exception;
}
