package qingzhou.detector.impl.strategy;

import qingzhou.detector.ApplicationProfile;
import qingzhou.detector.DetectionStrategy;
import qingzhou.detector.PathResult;
import qingzhou.detector.impl.PathDerivationUtil;
import qingzhou.detector.impl.PlatformUtil;
import java.io.File;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 4.应用命令反推策略
 *
 * 通过执行应用自身的版本/信息命令（如 nginx -V, httpd -V, catalina.sh version）， 从命令输出中提取安装路径相关字段，进而推导安装根目录。
 * 执行流程： 1. 尝试直接通过系统 PATH 调用命令 2. 若失败，通过 which/where 定位完整路径后再次尝试 3. 解析输出，正则提取路径，回溯验证确认文件
 *
 * 优先级: 40
 */
public class CommandStrategy implements DetectionStrategy {

    private static final int PRIORITY = 40;

    // 清理提取路径首尾引号的正则
    private static final Pattern QUOTE_CLEAN_PATTERN = Pattern.compile("^[\"'](.+)[\"']$");

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public List<PathResult> detect(ApplicationProfile profile) {
        List<String> exeNames = profile.getExecutableNames();
        String args = profile.getAppCommandArgs();

        if (exeNames == null || exeNames.isEmpty() || args == null || args.isEmpty()) {
            return Collections.emptyList();
        }

        File basePath = null;
        for (String exeName : exeNames) {
            // 1. 尝试直接通过 PATH 调用（如 nginx -V）
            PathResult result = tryExecute(exeName, args, profile);
            if (result != null) {
                return Collections.singletonList(result);
            }

            // 2. 若直接调用失败，通过系统命令(which/where)定位完整路径后再次尝试
            String fullPath;
            if (basePath == null || !new File(basePath, exeName).exists()) {
                fullPath = PlatformUtil.locateExecutable(exeName);
                basePath = new File(fullPath).getParentFile();
            } else {
                fullPath = new File(basePath, exeName).getAbsolutePath();
            }            
            if (fullPath != null && !fullPath.equalsIgnoreCase(exeName)) {
                result = tryExecute(fullPath, args, profile);
                if (result != null) {
                    return Collections.singletonList(result);
                }
            }
        }

        return Collections.emptyList();
    }

    // ==================== 命令执行与解析 ====================
    /**
     * 尝试执行命令并解析输出
     *
     * @param exe 可执行文件名或完整路径
     * @param args 版本命令参数
     * @param pattern 路径提取正则
     * @param profile 应用特征
     */
    private PathResult tryExecute(String exe, String args, ApplicationProfile profile) {
        try {
            List<String> lines;
            if (PlatformUtil.isMac()) {
                // 解决unix：脚本中存在if [ $have_tty -eq 1 ]; then然后echo输出，对于unix系统，这不是一个tty，所以这部分输出无法获取
                String execPath = PlatformUtil.locateUnix(exe);
                lines = PlatformUtil.exec("script", "-Fq", "/dev/null", "/bin/sh", "-c",
                        execPath + " " +args);
            } else {
                lines = PlatformUtil.exec(exe, args);
            }

            String extracted = profile.extractPath(lines);
            Path path = PathDerivationUtil.deriveInstallDir(Paths.get(extracted), profile);
            if (path != null) {
                return new PathResult(path, 50, this, "exe:" + exe + ", extracted:" + extracted);// 命令反推置信度为中（低于环境变量/进程/服务）
            }
            // 1. 尝试从提取路径（可能是可执行文件、配置文件或目录）推导安装根目录 PathDerivationUtil.deriveInstallDir(path, profile);
            // 2. 若提取路径本身就是安装目录（如 --prefix=/usr/local/nginx） PathDerivationUtil.hasConfirmatoryFiles(path, profile)
        } catch (Exception ignored) {
        }
        return null;
    }

    

    /**
     * 清理字符串首尾成对的单引号或双引号
     */
    private String cleanQuotes(String str) {
        if (str == null || str.length() < 2) {
            return str;
        }
        Matcher m = QUOTE_CLEAN_PATTERN.matcher(str);
        if (m.matches()) {
            return m.group(1);
        }
        return str;
    }
}
