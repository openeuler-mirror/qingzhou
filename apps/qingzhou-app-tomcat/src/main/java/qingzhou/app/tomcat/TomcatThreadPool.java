package qingzhou.app.tomcat;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.type.Page;
import qingzhou.api.type.Show;

@Model(code = "threadpool", order = 3, icon = "Coin",
        name = {"线程池", "en:Thread Pools"},
        info = {"Tomcat线程池监控", "en:Tomcat thread pool monitoring"})
public class TomcatThreadPool extends TomcatModelBase implements Page, Show {

    @ModelField(id = true, list = true, show = true, readonly = true,
            name = {"池名称", "en:Pool Name"},
            info = {"线程池名称", "en:Thread pool name"})
    public String name;

    @ModelField(list = true, show = true, readonly = true,
            name = {"池前缀", "en:Pool Name Prefix"},
            info = {"池前缀", "en:Thread pool prefix"})
    public String namePrefix;

    @ModelField(list = true, show = true, readonly = true, numeric = true,
            name = {"最大线程", "en:Max Threads"},
            info = {"配置的最大线程数", "en:Maximum configured threads"})
    public String maxThreads;

    @ModelField(list = true, show = true, readonly = true, numeric = true,
            name = {"最小空闲", "en:Min Spare"},
            info = {"最小空闲线程数", "en:Minimum spare threads"})
    public String minSpareThreads;

    private List<Map<String, String>> threadPools;

    @Override
    public List<String[]> page(int pageNum, int pageSize,
                               Map<String, String> query, String[] listFields) {
        loadThreadPools();
        List<Map<String, String>> filtered = filterByQuery(threadPools, query);
        return buildListResult(filtered, pageNum, pageSize, listFields);
    }

    @Override
    public int totalSize(Map<String, String> query) {
        loadThreadPools();
        return filterByQuery(threadPools, query).size();
    }

    @Override
    public boolean contains(String id) {
        loadThreadPools();
        return threadPools.stream().anyMatch(p -> p.get("name").equals(id));
    }

    @Override
    public Map<String, String> show(String id) {
        try {
            id = URLDecoder.decode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        loadThreadPools();

        String finalId = id;
        return threadPools.stream()
                .filter(p -> p.get("name").equals(finalId))
                .findFirst()
                .orElse(null);
    }

    private void loadThreadPools() {
        threadPools = new ArrayList<>();
        File serverXml = getServerXmlFile();
        if (serverXml != null && serverXml.exists()) {
            List<Properties> executors;
            try {
                executors = getXmlNodes(serverXml, "//Executor");
            } catch (Exception e) {
                return;
            }
            for (Properties e : executors) {
                Map<String, String> pool = new LinkedHashMap<>();
                pool.put("maxThreads", e.getProperty("maxThreads", "200"));
                pool.put("minSpareThreads", e.getProperty("minSpareThreads", "10"));
                pool.put("name", e.getProperty("name", "--"));
                pool.put("namePrefix", e.getProperty("namePrefix", "--"));
                threadPools.add(pool);
            }
        }
    }
}