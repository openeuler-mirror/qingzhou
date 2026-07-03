package qingzhou.app.redis.store;

import qingzhou.api.AppContext;
import qingzhou.app.redis.store.model.StoreEntry;
import qingzhou.json.Json;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

abstract class AbstractStore<T extends StoreEntry> {
    protected final Deque<T> entries = new ConcurrentLinkedDeque<>();
    protected final AppContext appContext;
    protected final Path storageFile;
    protected final AtomicLong sequence = new AtomicLong(0);

    protected AbstractStore(AppContext appContext, String fileName) {
        this.appContext = appContext;
        this.storageFile = resolveStorageFile(fileName);
        load();
    }

    private Path resolveStorageFile(String fileName) {
        File base = appContext.getBase();
        File dir = new File(base, StoreConstants.DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, fileName).toPath();
    }

    protected void addEntry(T entry) {
        if (entry == null) return;
        if (entry.getId() == null) {
            entry.setId(String.valueOf(sequence.incrementAndGet()));
        }
        if (entry.getTimestamp() <= 0) {
            entry.setTimestamp(System.currentTimeMillis());
        }
        entries.offerLast(entry);
        while (entries.size() > getMaxSize()) {
            entries.pollFirst();
        }
    }

    protected T findById(String id) {
        if (id == null) return null;
        for (T entry : entries) {
            if (id.equals(entry.getId())) {
                return entry;
            }
        }
        return null;
    }

    protected List<T> getAllReversed() {
        List<T> list = new ArrayList<>(entries);
        Collections.reverse(list);
        return list;
    }

    protected List<T> paginate(List<T> list, int pageNum, int pageSize) {
        int start = (pageNum - 1) * pageSize;
        if (start >= list.size()) return new ArrayList<>();
        int end = Math.min(start + pageSize, list.size());
        return list.subList(start, end);
    }

    protected void restoreSequence(String id) {
        try {
            long seq = Long.parseLong(id);
            sequence.updateAndGet(current -> Math.max(current, seq));
        } catch (NumberFormatException ignored) {
        }
    }

    protected void appendToFile(T entry) {
        try {
            Json json = appContext.getService(Json.class);
            if (json == null) return;
            String line = json.toJson(entry);
            Files.createDirectories(storageFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(storageFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(line);
                writer.newLine();
            }
        } catch (Exception e) {
        }
    }

    protected void loadAppendEntries(Class<T> entryClass) {
        if (!Files.exists(storageFile)) return;
        try (BufferedReader reader = Files.newBufferedReader(storageFile, StandardCharsets.UTF_8)) {
            Json json = appContext.getService(Json.class);
            if (json == null) return;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    T entry = json.fromJson(line, entryClass);
                    if (entry != null) {
                        entries.offerLast(entry);
                        restoreSequence(entry.getId());
                    }
                } catch (Exception e) {
                }
            }
            while (entries.size() > getMaxSize()) {
                entries.pollFirst();
            }
        } catch (Exception e) {
        }
    }

    protected void saveSnapshot(Object snapshot) {
        try {
            Json json = appContext.getService(Json.class);
            if (json == null) return;
            String data = json.toJson(snapshot);
            Files.write(storageFile, data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
        }
    }

    protected <S> S loadSnapshot(Class<S> snapshotClass) {
        if (!Files.exists(storageFile)) return null;
        try (BufferedReader reader = Files.newBufferedReader(storageFile, StandardCharsets.UTF_8)) {
            Json json = appContext.getService(Json.class);
            if (json == null) return null;
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return json.fromJson(sb.toString(), snapshotClass);
        } catch (Exception e) {
            return null;
        }
    }

    public Deque<T> getEntries() {
        return entries;
    }

    protected abstract int getMaxSize();
    protected abstract void load();
}
