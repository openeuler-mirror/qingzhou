package qingzhou.registry.web;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class MultipartStreamParserTest {
    private static final String BOUNDARY = "----qingzhouTestBoundary";
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    private final Set<String> fileFields = new HashSet<>();
    private File uploadBase;

    @BeforeMethod
    public void setUp() throws IOException {
        uploadBase = Files.createTempDirectory("multipart-test").toFile();
        fileFields.clear();
    }

    @AfterMethod
    public void tearDown() {
        delete(uploadBase);
    }

    @Test
    public void declaredFileFieldSavedAndTextFieldsParsed() throws Exception {
        fileFields.add("avatar");
        MultipartStreamParser parser = newParser();
        feed(parser, body(part("avatar", "a.txt", "hello"), part("name", null, "tom")), 4096);

        Assert.assertEquals(parser.getParameters().get("name"), "tom");
        Assert.assertNull(parser.getParameters().get("avatar"));
        Assert.assertTrue(parser.getUploadFileFields().contains("avatar"));
        List<String> paths = parser.getUploadFileMap().get("avatar");
        Assert.assertEquals(paths.size(), 1);
        Assert.assertEquals(read(new File(paths.get(0))), "hello");
    }

    @Test
    public void undeclaredFileFieldContentIsDiscarded() throws Exception {
        MultipartStreamParser parser = newParser();
        feed(parser, body(part("evil", "x.txt", "payload"), part("bio", "y.txt", "payload"), part("name", null, "tom")), 4096);

        Assert.assertNull(parser.getParameters().get("evil"));
        Assert.assertNull(parser.getParameters().get("bio"));
        Assert.assertTrue(parser.getUploadFileMap().isEmpty());
        Assert.assertTrue(parser.getUploadFileFields().isEmpty());
        Assert.assertEquals(parser.getParameters().get("name"), "tom");
    }

    @Test
    public void inlineBoundaryInsideFileIsNotTruncated() throws Exception {
        fileFields.add("avatar");
        String content = "abc--" + BOUNDARY + "def";
        MultipartStreamParser parser = newParser();
        feed(parser, body(part("avatar", "a.txt", content)), 4096);

        List<String> paths = parser.getUploadFileMap().get("avatar");
        Assert.assertEquals(read(new File(paths.get(0))), content);
    }

    @Test
    public void contentStartingWithBoundaryTextIsKept() throws Exception {
        fileFields.add("avatar");
        String content = "--" + BOUNDARY + "x";
        MultipartStreamParser parser = newParser();
        feed(parser, body(part("avatar", "a.txt", content)), 1);

        Assert.assertEquals(read(new File(parser.getUploadFileMap().get("avatar").get(0))), content);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void missingClosingBoundaryRejected() throws Exception {
        fileFields.add("avatar");
        byte[] body = part("avatar", "a.txt", "hello").getBytes(UTF_8);
        feed(new MultipartStreamParser(BOUNDARY, uploadBase, fileFields::contains), body, 4096);
    }

    @Test
    public void byteByByteChunksParse() throws Exception {
        fileFields.add("avatar");
        MultipartStreamParser parser = newParser();
        feed(parser, body(part("avatar", "a.txt", "hello"), part("name", null, ""), part("bio", null, "hi")), 1);

        Assert.assertEquals(read(new File(parser.getUploadFileMap().get("avatar").get(0))), "hello");
        Assert.assertEquals(parser.getParameters().get("name"), "");
        Assert.assertEquals(parser.getParameters().get("bio"), "hi");
    }

    @Test
    public void filenameWithSeparatorOrTraversalRejected() throws Exception {
        for (String name : Arrays.asList("a,b.txt", "a=b.txt", "../a.txt", "a/b.txt", "a\\b.txt", ".")) {
            try {
                feed(newParser(), body(part("avatar", name, "x")), 4096);
                Assert.fail("should reject file name: " + name);
            } catch (IllegalStateException expected) {
                // 预期拒绝
            }
        }
    }

    @Test
    public void utf8FilenameAccepted() throws Exception {
        fileFields.add("avatar");
        MultipartStreamParser parser = newParser();
        feed(parser, body(part("avatar", "中文.txt", "x")), 4096);

        Assert.assertEquals(parser.getUploadFileMap().get("avatar").size(), 1);
        Assert.assertEquals(new File(parser.getUploadFileMap().get("avatar").get(0)).getName(), "中文.txt");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void malformedBoundarySuffixRejected() throws Exception {
        byte[] body = ("--" + BOUNDARY + "X\r\n\r\n").getBytes(UTF_8);
        feed(newParser(), body, 4096);
    }

    @Test
    public void controlOrFormatCharInFilenameRejected() throws Exception {
        // NEL(C1 控制符)、RLO(方向覆盖)、零宽空格(FORMAT)
        for (String name : Arrays.asList("a\u0085b.txt", "a\u202Eb.txt", "a\u200Bb.txt")) {
            try {
                feed(newParser(), body(part("avatar", name, "x")), 4096);
                Assert.fail("should reject file name with control or format char");
            } catch (IllegalStateException expected) {
                // 预期拒绝
            }
        }
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void oversizedPartHeadersRejected() throws Exception {
        StringBuilder payload = new StringBuilder("--").append(BOUNDARY).append("\r\n");
        for (int i = 0; i < 600; i++) payload.append("X-Pad-").append(i).append(": value\r\n");
        payload.append("\r\nx\r\n--").append(BOUNDARY).append("--\r\n");
        feed(newParser(), payload.toString().getBytes(UTF_8), 4096);
    }

    @Test
    public void uploadDirPermissionRestricted() throws Exception {
        fileFields.add("avatar");
        MultipartStreamParser parser = newParser();
        feed(parser, body(part("avatar", "a.txt", "x")), 4096);

        Path dir = new File(parser.getUploadFileMap().get("avatar").get(0)).toPath().getParent();
        if (Files.getFileAttributeView(dir, PosixFileAttributeView.class) != null) { // 非 POSIX 平台不适用
            Assert.assertEquals(Files.getPosixFilePermissions(dir), PosixFilePermissions.fromString("rwx------"));
        }
    }

    @Test
    public void largeChunkWithHeadersAndContentParsed() throws Exception {
        fileFields.add("avatar");
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 20000; i++) content.append("0123456789"); // 200KB，触发头部缓存上限截断
        MultipartStreamParser parser = newParser();
        feed(parser, body(part("avatar", "a.txt", content.toString()), part("name", null, "tom")), 65536);

        Assert.assertEquals(read(new File(parser.getUploadFileMap().get("avatar").get(0))), content.toString());
        Assert.assertEquals(parser.getParameters().get("name"), "tom");
    }

    private MultipartStreamParser newParser() {
        return new MultipartStreamParser(BOUNDARY, uploadBase, fileFields::contains);
    }

    private static byte[] body(String... parts) {
        StringBuilder body = new StringBuilder("preamble\r\n");
        for (String part : parts) {
            body.append(part);
        }
        body.append("--").append(BOUNDARY).append("--\r\n");
        return body.toString().getBytes(UTF_8);
    }

    private static String part(String name, String fileName, String content) {
        StringBuilder part = new StringBuilder();
        part.append("--").append(BOUNDARY).append("\r\n");
        part.append("Content-Disposition: form-data; name=\"").append(name).append("\"");
        if (fileName != null) part.append("; filename=\"").append(fileName).append("\"");
        part.append("\r\nContent-Type: application/octet-stream\r\n\r\n");
        part.append(content).append("\r\n");
        return part.toString();
    }

    private static void feed(MultipartStreamParser parser, byte[] data, int chunkSize) throws IOException {
        for (int i = 0; i < data.length; i += chunkSize) {
            int len = Math.min(chunkSize, data.length - i);
            parser.feed(Arrays.copyOfRange(data, i, i + len), i + len == data.length);
        }
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), UTF_8);
    }

    private static void delete(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) delete(child);
        }
        file.delete();
    }
}
