package qingzhou.framework.api;

public interface ActionFilter {
    boolean doFilter(Request request, Response response, AppContext appContext) throws Exception;
}
