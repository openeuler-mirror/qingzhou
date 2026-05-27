package qingzhou.api;

public interface ActionFilter {
    void doFilter(Request request, FilterChain chain) throws Throwable;
}
