package qingzhou.app.nginx;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import qingzhou.api.ModelBase;
import qingzhou.api.Request;

public class NginxModelBase extends ModelBase {
    private static final long INSTALL_CACHE_MILLIS = 30_000;

    private volatile NginxInstall cachedInstall;
    private volatile long cachedAt;

    protected NginxInstall install() {
        NginxInstall current = cachedInstall;
        if (current != null && System.currentTimeMillis() - cachedAt < INSTALL_CACHE_MILLIS
                && Files.isRegularFile(current.binary) && Files.isRegularFile(current.conf)) {
            return current;
        }
        NginxInstall resolved = NginxInstall.resolve(getAppContext());
        cachedInstall = resolved;
        cachedAt = System.currentTimeMillis();
        return resolved;
    }

    protected NginxService service() {
        return new NginxService(install());
    }

    protected void testConfig(Request request) {
        execute(request, () -> {
            NginxCommand.Result result = service().test();
            if (result.isSuccess()) {
                request.getResponse().success(true)
                        .message("配置校验通过\n" + result.getOutput());
            } else {
                request.getResponse().error("配置校验失败\n" + result.toMessage());
            }
        });
    }

    protected File backupDir() {
        return new File(getAppContext().getTempDir(), "backups");
    }

    /**
     * 预期内的运行失败（未找到 nginx、nginx 命令失败、配置校验失败）转成业务错误响应，
     * 让管控台直接展示原因；NPE 等程序缺陷继续上抛由框架记录。
     */
    protected static void execute(Request request, Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException | IllegalArgumentException e) {
            request.getResponse().error(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    protected static boolean matches(Map<String, String> data, Map<String, String> query) {
        if (query == null || query.isEmpty()) return true;
        for (Map.Entry<String, String> entry : query.entrySet()) {
            String value = entry.getValue();
            if (value == null || value.trim().isEmpty()) continue;
            String actual = data.get(entry.getKey());
            if (actual == null) return false;
            if (!actual.toLowerCase(Locale.ROOT).contains(value.trim().toLowerCase(Locale.ROOT))) return false;
        }
        return true;
    }

    protected static <T> List<T> slice(List<T> values, int pageNum, int pageSize) {
        int size = Math.max(1, Math.min(pageSize, 100));
        int from = Math.max(0, (pageNum - 1) * size);
        if (from >= values.size()) return Collections.emptyList();
        return values.subList(from, Math.min(from + size, values.size()));
    }

    protected static String[] row(Map<String, String> data, String[] fields) {
        String[] row = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String value = data.get(fields[i]);
            row[i] = value == null ? "" : value;
        }
        return row;
    }
}
