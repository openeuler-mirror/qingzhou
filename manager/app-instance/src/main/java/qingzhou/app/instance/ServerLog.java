package qingzhou.app.instance;

import qingzhou.api.FieldType;
import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.api.ModelField;
import qingzhou.api.type.Downloadable;
import qingzhou.api.type.Updatable;
import qingzhou.deployer.DeployerConstants;
import qingzhou.engine.util.FileUtil;

import java.io.File;
import java.util.Collections;
import java.util.Map;

@Model(code = "serverlog", icon = "file-text",
        entrance = DeployerConstants.EDIT_ACTION,
        name = {"服务器日志", "en:Server Log"}, order = 3,
        info = {"QingZhou 实例的日志配置。", "en:The logging configuration of the QingZhou instance."})
public class ServerLog extends ModelBase implements Updatable, Downloadable {
    @ModelField(
            type = FieldType.select,
            options = {"rich", "simple", "none"},
            name = {"日志格式", "en:Log Pattern"},
            info = {"日志内容的记录格式，包括时间戳、线程名、日志级别等。", "en:The record format of the log content, including timestamp, thread name, log level, etc."})
    public String logPattern = "simple";

    @ModelField(
            type = FieldType.select,
            options = {"error", "warn", "info", "debug"},
            name = {"默认日志级别", "en:Global Log Level"},
            info = {"设置默认日志级别，只有在日志信息的严重程度等于或高于此级别时，才会记录日志。",
                    "en:Set the global log level so that log information is logged only if the severity of the log information is equal to or higher than this level."})
    private String logLevel = "info";

    @ModelField(
            type = FieldType.bool,
            name = {"开启异步日志", "en:Enable Asynchronous Logging"},
            info = {"日志记录可以在单独的线程中执行，好处是应用程序本身不会被缓慢的 IO 操作阻塞，写入线程以低优先级运行并与应用程序一起自动关闭。",
                    "en:Logging can be performed in a separate thread, the benefit is that the application itself is not blocked by slow IO operations, and the writing thread runs at low priority and automatically shuts down with the application."})
    public Boolean enableAsynchronousLogging = true;

    @ModelField(
            show = "enableAsynchronousLogging=true",
            type = FieldType.number,
            min = 1,
            name = {"缓冲队列大小", "en:Buffer Queue Size"},
            info = {"日志缓冲队列大小，单位为条数。缓冲队列所能容纳的日志字节大小的最大值为缓冲队列大小乘以每条日志的字节大小，可据此估计日志子系统所需要的内存峰值。",
                    "en:The size of the log buffer queue, the unit is the number of entries. The maximum size of log bytes that the buffer queue can hold is the size of the buffer queue multiplied by the size of each log in bytes, which can be used to estimate the peak memory required by the log subsystem."})
    private Integer bufferQueueSize = 100000;

    @ModelField(
            group = "file",
            type = FieldType.bool,
            name = {"按天轮转", "en:Rotation by Day"},
            info = {"将每日未达到大小阈值的日志在零点切割到一个新文件中。", "en:Cut the daily logs that do not reach the size threshold to a new file at zero."})
    private Boolean rotationByDay = true;

    @ModelField(
            group = "file",
            type = FieldType.number,
            min = 1,
            name = {"按大小轮转", "en:Rotation by Size"},
            info = {"当日志文件大小（单位：MB）达到该阈值时，将切割出一个新的日志文件。", "en:When the log file size (in MB) reaches this threshold, a new log file will be cut out."})
    private Integer rotationBySize = 50;

    @ModelField(
            group = "file",
            type = FieldType.number,
            min = 1,
            name = {"保留文件个数", "en:Keep Files"},
            info = {"指定在清理日志文件时，需保留文件的个数。", "en:Specifies the number of files to be retained when cleaning up log files."})
    private Integer keepMaxFiles = 100;

    @ModelField(
            group = "file",
            type = FieldType.select,
            options = {"UTF-8", "GBK", "ISO-8859-1", "GB18030", "GB2312", "UTF-16", "US-ASCII"},
            name = {"文件编码", "en:Charset"},
            info = {"指定日志文件的编码格式。", "en:Specifies the encoding format of the log file."})
    private String charset = "UTF-8";

    @ModelField(
            group = "file",
            type = FieldType.bool,
            name = {"日志文件压缩", "en:Log File Compression"},
            info = {"是否开启日志文件压缩功能。开启后，对轮转后的日志文件进行压缩。",
                    "en:If or not to enable the log file compression. When enabled, the log file is compressed after the rotation."})
    private boolean compression = false;

    @ModelField(
            group = "file",
            type = FieldType.bool,
            name = {"缓冲写入", "en:Buffered"},
            info = {"是否开启文件缓冲写入功能。开启后，当持续记录的日志内容大小（单位：字节）之和达到缓冲大小时才一次性写入文件，而不是每条日志都立即写入文件（在服务器停止时会全部写入），这通常可以提高日志记录性能。关闭后，每条日志都立即写入文件。",
                    "en:Whether to enable the file buffer write function. After it is enabled, when the sum of the continuously recorded log content size (unit: bytes) reaches the buffer size, the file will be written to the file at one time, instead of writing each log to the file immediately (all of them will be written when the server stops). This often improves logging performance. When closed, each log is written to the file immediately."})
    private boolean buffered = true;

    @ModelField(
            group = "file",
            show = "buffered=true",
            type = FieldType.number,
            min = 1,
            name = {"缓冲大小", "en:Buffered Size"},
            info = {"缓冲写入的阈值，当持续记录的日志内容大小（单位：字节）之和达到此值时，一次性写入进文件。"
                    , "en:The threshold for buffered writing. When the sum of the size (unit: bytes) of the log content that is continuously recorded reaches this value, the file will be written to the file at one time."})
    private Integer bufferedSize = 64 * 1024; // 64 KB

    @Override
    public File downloadData(String id) {
        return FileUtil.newFile(Main.getInstanceDir(), "logs");
    }

    @Override
    public void updateData(Map<String, String> data) throws Exception {
        // todo 写入配置到日志模块配置文件里，并立即生效
    }

    @Override
    public Map<String, String> showData(String id) {
        return Collections.emptyMap();// todo 日志模块获取
    }
}
