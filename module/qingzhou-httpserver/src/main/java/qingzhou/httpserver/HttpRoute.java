package qingzhou.httpserver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HttpRoute {

    private final String url;
    private Set<String> methods = new HashSet<String>() {{
        add("GET");
        add("HEAD");
        add("POST");
        add("PUT");
        add("DELETE");
    }};
    
    public HttpRoute(String url) {
        this.url = url;
    }

    public HttpRoute(String url, String... methods) {
        this.url = url;
        Set<String> sets = new HashSet<>();
        for (String item : methods) {
            if (this.methods.contains(item.toUpperCase())) {
                sets.add(item.toUpperCase());
            }
        }
        this.methods = sets;
    }

    public Set<String> getMethod() {
        return this.methods;
    }

    public String getUrl() {
        return url;
    }
    
    public HttpRoute match(List<HttpRoute> routes) {
        if (routes == null || routes.isEmpty()) {
            return null;
        }
        HttpRoute find = null;
        int matchScore = Integer.MAX_VALUE;
        for (HttpRoute item : routes) {
            if (item.getUrl().startsWith(this.url) && item.getMethod().containsAll(this.methods)) {
                if (find == null || (item.getUrl().length() - this.url.length()) < matchScore) {
                    find = item;
                    matchScore = item.getUrl().length() - this.url.length();
                }
            }
        }
        
        return find;
    }
}
