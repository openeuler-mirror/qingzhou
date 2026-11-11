package qingzhou.engine.detector;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

// 应用实例序号会进入应用编码与配置主键（见 AppDriver），必须跨探测与跨重启稳定；
// 探测结果顺序并不稳定（listFiles 与 ps 输出顺序均未定义），故不能按列表位次推导，
// 改为"路径→序号"显式映射：新路径按字典序取最小空闲序号，消失路径的序号被回收，
// 映射持久化到实例 temp 目录，与 osgi-cache（bundle location、配置）同生共灭，避免重启后序号错位到别的应用
class PathSeqAllocator {
    private final Path storeFile;
    private final Map<String, Integer> seqByPath = new HashMap<>();

    PathSeqAllocator(String instanceDir, String symbolicName) throws IOException {
        this.storeFile = Paths.get(instanceDir, "temp", "path-detection", symbolicName + ".properties");
        load();
    }

    // 返回本次探测结果的 路径→序号；不再探测到的路径释放其序号
    Map<String, Integer> assign(List<String> detectedPaths) throws IOException {
        Set<String> detected = new HashSet<>(detectedPaths);
        boolean changed = seqByPath.keySet().retainAll(detected);

        Set<Integer> used = new HashSet<>(seqByPath.values());
        List<String> added = new ArrayList<>();
        for (String path : detected) {
            if (!seqByPath.containsKey(path)) added.add(path);
        }
        // 同一拍内新增多个路径时按字典序取号，使分配结果与探测顺序无关
        Collections.sort(added);
        for (String path : added) {
            int seq = 0;
            while (used.contains(seq)) seq++;
            used.add(seq);
            seqByPath.put(path, seq);
            changed = true;
        }
        if (changed) persist();

        Map<String, Integer> assigned = new HashMap<>();
        for (String path : detected) {
            assigned.put(path, seqByPath.get(path));
        }
        return assigned;
    }

    private void load() throws IOException {
        if (!Files.exists(storeFile)) return;

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(storeFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        for (String seq : properties.stringPropertyNames()) {
            seqByPath.put(properties.getProperty(seq), Integer.valueOf(seq));
        }
    }

    private void persist() throws IOException {
        Files.createDirectories(storeFile.getParent());
        Properties properties = new Properties();
        for (Map.Entry<String, Integer> entry : seqByPath.entrySet()) {
            properties.setProperty(String.valueOf(entry.getValue()), entry.getKey());
        }
        try (Writer writer = Files.newBufferedWriter(storeFile, StandardCharsets.UTF_8)) {
            properties.store(writer, null);
        }
    }
}
