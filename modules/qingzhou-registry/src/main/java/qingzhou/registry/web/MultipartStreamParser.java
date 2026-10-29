package qingzhou.registry.web;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.*;

import qingzhou.common.FileUtil;

class MultipartStreamParser {
    private static final int MAX_PART_HEADERS = 8192; // 单个分片的头部上限
    private static final int MAX_FIELD_BYTES = 1024 * 1024; // 单个文本字段上限
    // 文本字段累计上限：单字段上限 × 分片总数可达 GB 级并全部驻留堆，须按总量独立约束（文件分片落盘，不计入）
    private static final long MAX_TOTAL_TEXT_BYTES = 8L * 1024 * 1024;
    private static final int MAX_TOTAL_PARTS = 1000; // 分片总数上限：防小分片洪水放大内存
    private static final byte[] HEADER_DELIMITER = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);
    private static final int[] HEADER_DELIMITER_FAILURE = failure(HEADER_DELIMITER);

    private final byte[] boundaryDelimiter;
    private final int[] boundaryFailure;

    private final File uploadBase;
    private final FieldTypeResolver fieldTypeResolver;

    private enum State {BEFORE_FIRST, AFTER_BOUNDARY, IN_HEADERS, IN_CONTENT, DONE}

    private State state = State.BEFORE_FIRST;

    private final byte[] leftover;
    private int leftoverLen;

    private byte[] headerBytes = new byte[0];
    private int headerLen;
    private final byte[] boundarySuffix = new byte[2];
    private int suffixLen;

    private int partCount;
    private long totalTextBytes;
    private String currentFieldName;
    private boolean isFileField;
    private boolean skipPart; // 文件名存在但字段未声明为文件：内容丢弃，既不落盘也不作为文本参数

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
        this.boundaryFailure = failure(boundaryDelimiter);
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
                case AFTER_BOUNDARY:
                    pos = handleAfterBoundary(searchArea, pos);
                    break;
                case IN_HEADERS:
                    pos = handleHeaders(searchArea, pos);
                    break;
                case IN_CONTENT:
                    pos = handleContent(searchArea, pos);
                    break;
            }
        }
        // 缺少结束边界说明请求体被截断：不得把最后一片当完整内容交给业务
        if (isLast && state != State.DONE) {
            throw new IllegalStateException("unexpected end of multipart body");
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
        int idx = indexOf(data, pos, data.length, boundaryDelimiter, boundaryFailure);
        if (idx < 0) {
            int safe = data.length - pos - boundaryDelimiter.length;
            if (safe > 0) pos += safe;
            saveLeftover(data, pos);
            return data.length;
        }
        state = State.AFTER_BOUNDARY;
        suffixLen = 0;
        return idx + boundaryDelimiter.length;
    }

    // 边界之后只允许 CRLF（下一分片）或 "--"（结束）：逐字节读取，兼容分隔符跨块拆分
    private int handleAfterBoundary(byte[] data, int pos) {
        while (suffixLen < boundarySuffix.length && pos < data.length) {
            boundarySuffix[suffixLen++] = data[pos++];
        }
        if (suffixLen < boundarySuffix.length) return data.length;

        if (boundarySuffix[0] == '-' && boundarySuffix[1] == '-') {
            state = State.DONE;
            return data.length;
        }
        if (boundarySuffix[0] != '\r' || boundarySuffix[1] != '\n') {
            throw new IllegalStateException("malformed multipart boundary");
        }
        state = State.IN_HEADERS;
        return pos;
    }

    private int handleHeaders(byte[] data, int pos) throws IOException {
        int oldLen = headerLen;
        // 只缓存头部上限所需的字节：多余正文由返回值回退给 content 处理，避免整块复制与 headerBytes 无界扩容
        int appendLen = Math.min(data.length - pos, MAX_PART_HEADERS + HEADER_DELIMITER.length - oldLen);
        if (appendLen > 0) appendHeader(data, pos, appendLen);
        int idx = indexOf(headerBytes, 0, headerLen, HEADER_DELIMITER, HEADER_DELIMITER_FAILURE);
        if (idx < 0) {
            if (headerLen > MAX_PART_HEADERS) {
                throw new IllegalStateException("multipart part headers too large");
            }
            return data.length;
        }
        if (idx > MAX_PART_HEADERS) {
            throw new IllegalStateException("multipart part headers too large");
        }

        parsePartHeaders(new String(headerBytes, 0, idx, StandardCharsets.UTF_8));
        headerLen = 0;
        state = State.IN_CONTENT;
        return pos + idx + HEADER_DELIMITER.length - oldLen;
    }

    private int handleContent(byte[] data, int pos) throws IOException {
        int boundaryPos = findBoundary(data, pos);
        if (boundaryPos >= 0) {
            writeContent(data, pos, boundaryPos - 2 - pos); // 分隔符前的 CRLF 属分隔符，不计入内容
            finishCurrentPart();
            state = State.AFTER_BOUNDARY;
            suffixLen = 0;
            return boundaryPos + boundaryDelimiter.length;
        }

        int safeLen = data.length - pos - boundaryDelimiter.length - 4;
        if (safeLen > 0) {
            writeContent(data, pos, safeLen);
            pos += safeLen;
        }
        saveLeftover(data, pos);
        return data.length;
    }

    // 仅行首（前有 CRLF）的边界才是分隔符：否则文件内容中的 "--boundary" 会截断分片
    private int findBoundary(byte[] data, int from) {
        int matched = 0;
        for (int i = from; i < data.length; i++) {
            while (matched > 0 && data[i] != boundaryDelimiter[matched]) matched = boundaryFailure[matched - 1];
            if (data[i] == boundaryDelimiter[matched]) matched++;
            if (matched == boundaryDelimiter.length) {
                int start = i - boundaryDelimiter.length + 1;
                if (start >= 2 && data[start - 2] == '\r' && data[start - 1] == '\n') {
                    return start;
                }
                matched = boundaryFailure[matched - 1]; // 非分隔符，继续寻找下一个候选
            }
        }
        return -1;
    }

    private void writeContent(byte[] data, int offset, int len) throws IOException {
        if (len <= 0 || skipPart) return;
        if (isFileField) {
            fileStream.write(data, offset, len);
        } else {
            if (fieldBuffer.size() + len > MAX_FIELD_BYTES) {
                throw new IllegalStateException("multipart field too large: " + currentFieldName);
            }
            totalTextBytes += len;
            if (totalTextBytes > MAX_TOTAL_TEXT_BYTES) {
                throw new IllegalStateException("multipart text fields exceed max total bytes: " + MAX_TOTAL_TEXT_BYTES);
            }
            fieldBuffer.write(data, offset, len);
        }
    }

    private void finishCurrentPart() throws IOException {
        if (isFileField) {
            fileStream.close();
            fileStream = null;
            uploadFileMap.computeIfAbsent(currentFieldName, k -> new ArrayList<>())
                    .add(currentTempFile.getAbsolutePath());
            uploadFileFields.add(currentFieldName);
        } else if (currentFieldName != null && !skipPart) {
            parameters.put(currentFieldName, fieldBuffer.toString("UTF-8").trim());
        }
        // fieldBuffer 须无条件复位：filename 存在而 name 缺失的 part 走文本分支时，
        // 否则其残留内容会拼进下一个文本字段的值（同请求内参数污染）
        fieldBuffer.reset();
        currentFieldName = null;
        isFileField = false;
        skipPart = false;
    }

    /**
     * 中断解析时调用：关闭未完成的文件流并清理已写入的临时文件，防止句柄与临时文件残留。此方法可重复调用。
     */
    void abort() {
        if (fileStream != null) {
            try {
                fileStream.close();
            } catch (IOException ignored) {
            }
            fileStream = null;
        }
        deleteUploadedFile(currentTempFile);
        currentTempFile = null;
        for (List<String> paths : uploadFileMap.values()) {
            for (String path : paths) {
                deleteUploadedFile(new File(path));
            }
        }
        uploadFileMap.clear();
        uploadFileFields.clear();
        state = State.DONE;
    }

    private void deleteUploadedFile(File tempFile) {
        if (tempFile == null) return;
        File parentDir = tempFile.getParentFile();
        if (parentDir != null && parentDir.getParentFile() != null
                && parentDir.getParentFile().equals(uploadBase)) {
            tempFile.delete();
            parentDir.delete();
        }
    }

    private void parsePartHeaders(String headers) throws IOException {
        if (++partCount > MAX_TOTAL_PARTS) {
            throw new IllegalStateException("too many multipart parts");
        }
        String disposition = contentDisposition(headers);
        currentFieldName = extractValue(disposition, "name=\"");
        if (currentFieldName != null && currentFieldName.isEmpty()) currentFieldName = null;

        String currentFileName = extractValue(disposition, "filename=\"");
        if (currentFileName != null && !currentFileName.isEmpty()) {
            validateFileName(currentFileName);
            isFileField = currentFieldName != null && fieldTypeResolver.isFileField(currentFieldName);
            // 未声明为文件字段时只丢弃内容：既不能落盘，也不能让文件内容变成业务可读的文本参数
            skipPart = !isFileField;
        }
        if (isFileField) {
            String uploadId = UUID.randomUUID().toString();
            File uploadDir = new File(uploadBase, uploadId);
            // 上传内容可能含敏感数据：创建即收紧为 0700，避免默认 umask 下同机其它用户列举目录、经文件名泄露内容
            FileUtil.createOwnerOnlyDir(uploadDir);
            currentTempFile = new File(uploadDir, currentFileName);
            fileStream = FileUtil.newOwnerOnlyFile(currentTempFile.toPath(), StandardOpenOption.CREATE_NEW);
        }
    }

    // 文件名参与落盘与远端文件列表（逗号、等号是列表分隔符），须拒绝路径穿越、控制字符与协议字符
    private static void validateFileName(String fileName) {
        boolean invalid = fileName.equals(".") || fileName.trim().isEmpty() || fileName.contains("..")
                || fileName.getBytes(StandardCharsets.UTF_8).length > 200;
        for (int i = 0; !invalid && i < fileName.length(); i++) {
            char c = fileName.charAt(i);
            // ISO 控制字符与 Unicode 格式字符（如 RLO）可用于日志注入与文件名显示欺骗
            invalid = Character.isISOControl(c) || Character.getType(c) == Character.FORMAT
                    || c == '/' || c == '\\' || c == '=' || c == ',';
        }
        if (invalid) throw new IllegalStateException("invalid upload file name");
    }

    // 只认 Content-Disposition 行：其它头部出现的 name="..." 不参与字段识别
    private static String contentDisposition(String headers) {
        for (String line : headers.split("\r\n")) {
            if (line.regionMatches(true, 0, "content-disposition:", 0, "content-disposition:".length())) {
                return line;
            }
        }
        return "";
    }

    // 值提取：要求 key 之前不是参数名组成字符，否则 "name=\"" 会命中 "filename=\""，把文件名当成字段名
    private String extractValue(String headers, String key) {
        int from = 0;
        while (true) {
            int start = headers.indexOf(key, from);
            if (start == -1) return null;
            if (start == 0 || !isParameterNameChar(headers.charAt(start - 1))) {
                int valueStart = start + key.length();
                int end = headers.indexOf('"', valueStart);
                return end == -1 ? null : headers.substring(valueStart, end);
            }
            from = start + 1;
        }
    }

    private static boolean isParameterNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.';
    }

    private void appendHeader(byte[] data, int pos, int len) {
        if (headerLen + len > headerBytes.length) {
            headerBytes = Arrays.copyOf(headerBytes, Math.max(headerLen + len, headerBytes.length * 2));
        }
        System.arraycopy(data, pos, headerBytes, headerLen, len);
        headerLen += len;
    }

    private void saveLeftover(byte[] data, int pos) {
        leftoverLen = data.length - pos;
        if (leftoverLen > 0) System.arraycopy(data, pos, leftover, 0, leftoverLen);
    }

    // KMP：boundary 客户端可控（最长 70 字符），朴素匹配在最坏内容下可达约 70 次比较/字节
    private static int indexOf(byte[] data, int from, int to, byte[] pattern, int[] failure) {
        int matched = 0;
        for (int i = from; i < to; i++) {
            while (matched > 0 && data[i] != pattern[matched]) matched = failure[matched - 1];
            if (data[i] == pattern[matched]) matched++;
            if (matched == pattern.length) return i - pattern.length + 1;
        }
        return -1;
    }

    private static int[] failure(byte[] pattern) {
        int[] failure = new int[pattern.length];
        for (int i = 1, matched = 0; i < pattern.length; i++) {
            while (matched > 0 && pattern[i] != pattern[matched]) matched = failure[matched - 1];
            if (pattern[i] == pattern[matched]) failure[i] = ++matched;
        }
        return failure;
    }
}
