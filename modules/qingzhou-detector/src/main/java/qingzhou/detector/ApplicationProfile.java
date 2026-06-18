package qingzhou.detector;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 应用特征接口（各应用模块只需实现此接口）
 */
public interface ApplicationProfile {

    // Profile 名称
    String getName();

    // 用于控制探测返回策略（true 表示探测到停止后续探测策略 | false 表示执行所有探测策略）
    default boolean isStopOnHit() {
        return true;
    }

    // --- 环境变量 ---
    // 环境变量名称，如 ["CATALINA_HOME", "CATALINA_BASE"]
    default List<String> getEnvVarKeys() {
        return new ArrayList<>();
    }

    // --- 进程特征 ---
    // 匹配进程名或命令行关键字，如 ["nginx"] 或 ["catalina", "tomcat"]
    List<String> getProcessIdentifiers();

    // 从进程启动命令中提取安装路径
    default String extractPathByCmdArgs(String cmdArgs) {
        return "";
    }

    // --- 服务特征 ---
    // 服务名关键字，如 ["Tomcat9", "tomcat"]
    default List<String> getServiceIdentifiers() {
        return new ArrayList<>();
    }

    // --- 命令反推特征 ---
    // 可执行文件名，如 ["nginx", "nginx.exe"]
    default List<String> getExecutableNames() {
        return new ArrayList<>();
    }

    // 应用可执行文件参数，如 "-V" 或 "version"，用于通过命令参数输出应用信息反推应用目录
    default String getAppCommandArgs() {
        return "";
    }

    // 从命令输出提取安装路径
    default String extractPath(List<String> cmdOutput) {
        return "";
    }

    // --- 候选路径扫描特征 ---
    // 目录名包含的标识，如 ["tomcat", "apache-tomcat"]
    List<String> getDirNameMatches();

    // 确认安装目录的关键相对路径，如 ["sbin/nginx", "conf/nginx.conf"]
    List<String> getConfirmatoryFiles();

    // --- 扩展辅助方法 ---
    // 用于去重(部分程序存在多个进程或系统运行多个程序，需要根据实际程序决定是否需要去重)
    default void deduplicate(List<PathResult> results) {
    }

    // 自定义常用的应用程序候选目录(用于候选目录进行扫描)
    default List<Path> getCustomCandidatePath() {
        return new ArrayList<>();
    }
}
