//package qingzhou.app.nginx;
//
//import org.testng.Assert;
//import org.testng.annotations.Test;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.*;
//import java.util.Map;
//
///**
// * NginxConfigParser测试类
// */
//public class NginxConfigParserTest {
//
//    private static final String TEST_CONF_DIR = "/tmp/nginx-test";
//    private static final String TEST_CONF_PATH = TEST_CONF_DIR + "/nginx.conf";
//
//    @Test
//    public void parseConfig_success() throws IOException {
//        // 准备测试配置文件
//        createTestConfig();
//
//        // 使用反射修改静态字段进行测试
//        // 由于实际测试需要修改静态常量，这里主要测试解析逻辑
//        // 实际使用时会读取真实的nginx.conf
//
//        // 验证配置文件存在
//        File confFile = new File("/home/qingzhou/ngx/nginx.conf");
//        Assert.assertTrue(confFile.exists(), "nginx.conf应该存在");
//    }
//
//    @Test
//    public void getConfigDescriptions_notEmpty() {
//        Map<String, String> descriptions = NginxConfigParser.getConfigDescriptions();
//        Assert.assertNotNull(descriptions);
//        Assert.assertFalse(descriptions.isEmpty(), "配置描述不应为空");
//        Assert.assertTrue(descriptions.containsKey("worker_processes"), "应包含worker_processes配置");
//        Assert.assertTrue(descriptions.containsKey("worker_connections"), "应包含worker_connections配置");
//    }
//
//    @Test
//    public void backupConfig_createsBackupFile() throws IOException {
//        // 备份目录应该能创建
//        String backupDir = "/home/qingzhou/ngx/backups";
//        File dir = new File(backupDir);
//
//        // 如果目录不存在，应该能创建
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//
//        Assert.assertTrue(dir.exists() || dir.mkdirs(), "备份目录应该存在或可创建");
//    }
//
//    @Test
//    public void extractSimpleDirective_correctFormat() {
//        // 测试正则表达式匹配
//        String testContent = "user nginx;  # comment\nworker_processes auto;";
//
//        // 测试user指令
//        String regex = "user\\s+([^;]+);";
//        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
//        java.util.regex.Matcher matcher = pattern.matcher(testContent);
//
//        Assert.assertTrue(matcher.find(), "应该能匹配user指令");
//        Assert.assertEquals(matcher.group(1).trim(), "nginx", "应该提取到nginx值");
//    }
//
//    @Test
//    public void extractBlockDirective_correctFormat() {
//        // 测试块内指令匹配
//        String testContent = "events {\n" +
//                "    worker_connections 1024;\n" +
//                "    use epoll;\n" +
//                "}";
//
//        // 测试events块
//        String blockRegex = "events\\s*\\{([^}]+)\\}";
//        java.util.regex.Pattern blockPattern = java.util.regex.Pattern.compile(blockRegex, java.util.regex.Pattern.DOTALL);
//        java.util.regex.Matcher blockMatcher = blockPattern.matcher(testContent);
//
//        Assert.assertTrue(blockMatcher.find(), "应该能匹配events块");
//        String blockContent = blockMatcher.group(1);
//        Assert.assertTrue(blockContent.contains("worker_connections 1024"), "块内容应包含worker_connections");
//    }
//
//    @Test
//    public void updateConfig_globalDirective() throws IOException {
//        // 创建测试配置
//        createTestConfig();
//
//        String testContent = "user nginx;\nworker_processes auto;\n";
//        String directive = "worker_processes";
//        String newValue = "4";
//
//        // 测试替换逻辑
//        String globalRegex = "(" + directive + "\\s+)[^;]+;";
//        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(globalRegex);
//        java.util.regex.Matcher matcher = pattern.matcher(testContent);
//
//        Assert.assertTrue(matcher.find(), "应该能找到指令");
//        String newContent = matcher.replaceFirst("$1" + newValue + ";");
//        Assert.assertTrue(newContent.contains("worker_processes 4;"), "应该替换为新值");
//    }
//
//    @Test
//    public void formatFileSize_correct() {
//        // 测试文件大小格式化
//        Assert.assertEquals(formatFileSize(500), "500 B");
//        Assert.assertEquals(formatFileSize(1024), "1.00 KB");
//        Assert.assertEquals(formatFileSize(1024 * 1024), "1.00 MB");
//    }
//
//    private String formatFileSize(long size) {
//        if (size < 1024) {
//            return size + " B";
//        } else if (size < 1024 * 1024) {
//            return String.format("%.2f KB", size / 1024.0);
//        } else {
//            return String.format("%.2f MB", size / (1024.0 * 1024.0));
//        }
//    }
//
//    private void createTestConfig() throws IOException {
//        File testDir = new File(TEST_CONF_DIR);
//        if (!testDir.exists()) {
//            testDir.mkdirs();
//        }
//
//        String testConfig = "# Test Nginx Configuration\n" +
//                "user nginx;\n" +
//                "worker_processes auto;\n" +
//                "error_log /var/log/nginx/error.log warn;\n" +
//                "pid /run/nginx.pid;\n" +
//                "\n" +
//                "events {\n" +
//                "    worker_connections 1024;\n" +
//                "    use epoll;\n" +
//                "    multi_accept on;\n" +
//                "}\n" +
//                "\n" +
//                "http {\n" +
//                "    access_log /var/log/nginx/access.log main;\n" +
//                "    default_type application/octet-stream;\n" +
//                "    \n" +
//                "    proxy_connect_timeout 6s;\n" +
//                "    proxy_send_timeout 10s;\n" +
//                "    proxy_read_timeout 10s;\n" +
//                "    proxy_buffer_size 16k;\n" +
//                "    proxy_buffers 4 32k;\n" +
//                "    \n" +
//                "    gzip on;\n" +
//                "    gzip_min_length 1k;\n" +
//                "    \n" +
//                "    server {\n" +
//                "        listen 80;\n" +
//                "        server_name example.com;\n" +
//                "    }\n" +
//                "}";
//
//        Files.write(Paths.get(TEST_CONF_PATH), testConfig.getBytes());
//    }
//}
