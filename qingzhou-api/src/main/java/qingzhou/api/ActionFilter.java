package qingzhou.api;

public interface ActionFilter {
    String doFilter(Request request, Response response, AppContext appContext) throws Exception;
}
