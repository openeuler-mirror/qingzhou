package qingzhou.registry.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

class MultipartStreamParser {
    private final byte[] boundaryDelimiter;
    private final byte[] HEADER_DELIMITER = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);

    private final File uploadBase;
    private final FieldTypeResolver fieldTypeResolver;

    private enum State {BEFORE_FIRST, IN_HEADERS, IN_CONTENT, DONE}

    private State state = State.BEFORE_FIRST;

    private final byte[] leftover;
    private int leftoverLen;

    private final ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();

    private String currentFieldName;
    private String currentFileName;
    private boolean isFileField;

    private final ByteArrayOutputStream fieldBuffer = new ByteArrayOutputStream();
    private File currentTempFile;
    private OutputStream fileStream;

    private final Map<String, String> parameters = new LinkedHashMap<>();
    private final Map<String, List<String>> uploadFileMap = new LinkedHashMap<>();
    private final Set<String> uploadFileFields = new LinkedHashSet<>();

    interface FieldTypeResolver {
        boolean isFileField(String fieldName);
    }

    MultipartStreamParser(String boundary, File uploadBase, FieldTypeResolver resolver) {
        this.boundaryDelimiter = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        this.uploadBase = uploadBase;
        this.fieldTypeResolver = resolver;
        this.leftover = new byte[boundaryDelimiter.length + 4];
    }

    void feed(byte[] data, boolean isLast) throws IOException {
        if (state == State.DONE) return;

        byte[] searchArea;
        if (leftoverLen > 0) {
            searchArea = new byte[leftoverLen + data.length];
            System.arraycopy(leftover, 0, searchArea, 0, leftoverLen);
            System.arraycopy(data, 0, searchArea, leftoverLen, data.length);
            leftoverLen = 0;
        } else {
            searchArea = data;
        }

        int pos = 0;
        while (pos < searchArea.length && state != State.DONE) {
            switch (state) {
                case BEFORE_FIRST:
                    pos = handlePreamble(searchArea, pos);
                    break;
                case IN_HEADERS:
                    pos = handleHeaders(searchArea, pos);
                    break;
                case IN_CONTENT:
                    pos = handleContent(searchArea, pos, isLast);
                    break;
            }
        }
    }

    Map<String, String> getParameters() {
        return parameters;
    }

    Map<String, List<String>> getUploadFileMap() {
        return uploadFileMap;
    }

    Set<String> getUploadFileFields() {
        return uploadFileFields;
    }

    private int handlePreamble(byte[] data, int pos) {
        int idx = indexOf(data, boundaryDelimiter, pos);
        if (idx < 0) {
            int safe = data.length - pos - boundaryDelimiter.length;
            if (safe > 0) pos += safe;
            saveLeftover(data, pos);
            return data.length;
        }
        int after = idx + boundaryDelimiter.length;
        if (after < data.length && data[after] == '\r' && after + 1 < data.length && data[after + 1] == '\n')
            after += 2;
        if (after < data.length && data[after] == '-' && after + 1 < data.length && data[after + 1] == '-') {
            state = State.DONE;
            return data.length;
        }
        state = State.IN_HEADERS;
        return after;
    }

    private int handleHeaders(byte[] data, int pos) {
        int idx = indexOf(data, HEADER_DELIMITER, pos);
        if (idx < 0) {
            headerBuffer.write(data, pos, data.length - pos);
            return data.length;
        }
        int end = idx + HEADER_DELIMITER.length;
        String headers;
        if (headerBuffer.size() > 0) {
            headerBuffer.write(data, pos, idx - pos);
            headers = headerBuffer.toString();
            headerBuffer.reset();
        } else {
            headers = new String(data, pos, idx - pos, StandardCharsets.UTF_8);
        }
        parsePartHeaders(headers);
        state = State.IN_CONTENT;
        return end;
    }

    private int handleContent(byte[] data, int pos, boolean isLast) throws IOException {
        int boundaryPos = indexOf(data, boundaryDelimiter, pos);

        if (boundaryPos >= 0) {
            int contentEnd = boundaryPos;
            if (contentEnd >= 2 && data[contentEnd - 2] == '\r' && data[contentEnd - 1] == '\n')
                contentEnd -= 2;
            writeContent(data, pos, contentEnd - pos);
            finishCurrentPart();

            int after = boundaryPos + boundaryDelimiter.length;
            if (after + 1 < data.length && data[after] == '-' && data[after + 1] == '-') {
                state = State.DONE;
                return data.length;
            }
            if (after + 1 < data.length && data[after] == '\r' && data[after + 1] == '\n')
                after += 2;
            state = State.IN_HEADERS;
            return after;
        }

        if (isLast) {
            writeContent(data, pos, data.length - pos);
            finishCurrentPart();
            state = State.DONE;
            return data.length;
        }

        int safeLen = data.length - pos - boundaryDelimiter.length - 4;
        if (safeLen > 0) {
            writeContent(data, pos, safeLen);
            pos += safeLen;
        }
        saveLeftover(data, pos);
        return data.length;
    }

    private void writeContent(byte[] data, int offset, int len) throws IOException {
        if (len <= 0) return;
        if (isFileField) {
            fileStream.write(data, offset, len);
        } else {
            fieldBuffer.write(data, offset, len);
        }
    }

    private void finishCurrentPart() throws IOException {
        if (currentFieldName == null) return;
        if (isFileField) {
            fileStream.close();
            uploadFileMap.computeIfAbsent(currentFieldName, k -> new ArrayList<>())
                    .add(currentTempFile.getAbsolutePath());
            uploadFileFields.add(currentFieldName);
        } else {
            parameters.put(currentFieldName, fieldBuffer.toString("UTF-8").trim());
            fieldBuffer.reset();
        }
        currentFieldName = null;
        currentFileName = null;
        isFileField = false;
    }

    private void parsePartHeaders(String headers) {
        currentFieldName = extractValue(headers, "name=\"");
        currentFileName = extractValue(headers, "filename=\"");
        if (currentFileName != null && !currentFileName.isEmpty()
                && !currentFileName.contains("..")
                && !currentFileName.contains("/")
                && !currentFileName.contains("\\")) {
            isFileField = currentFieldName != null && fieldTypeResolver.isFileField(currentFieldName);
        }
        if (isFileField) {
            String uploadId = UUID.randomUUID().toString();
            File uploadDir = new File(uploadBase, uploadId);
            if (!uploadDir.mkdirs()) throw new IllegalStateException(uploadDir.getAbsolutePath());
            currentTempFile = new File(uploadDir, currentFileName);
            try {
                fileStream = Files.newOutputStream(currentTempFile.toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String extractValue(String headers, String key) {
        int start = headers.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        int end = headers.indexOf('"', start);
        if (end == -1) return null;
        return headers.substring(start, end);
    }

    private void saveLeftover(byte[] data, int pos) {
        leftoverLen = data.length - pos;
        if (leftoverLen > 0) System.arraycopy(data, pos, leftover, 0, leftoverLen);
    }

    private int indexOf(byte[] data, byte[] pattern, int startPos) {
        for (int i = startPos; i <= data.length - pattern.length; i++) {
            boolean found = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return i;
        }
        return -1;
    }
}