package qingzhou.console.master.security;

import qingzhou.api.console.FieldType;
import qingzhou.api.console.Model;
import qingzhou.api.console.ModelField;
import qingzhou.api.console.model.EditModel;
import qingzhou.console.master.MasterModelBase;
import qingzhou.console.util.Constants;

@Model(name = Constants.MODEL_NAME_auditconfig,
        icon = "checked", entryAction = EditModel.ACTION_NAME_EDIT,
        menuName = "Security", menuOrder = 7,
        nameI18n = {"审计配置", "en:Audit Config"},
        infoI18n = {"配置审计相关的功能，如审计日志。", "en:Configure audit-related functions, such as audit logging."})
public class AuditConfig extends MasterModelBase implements EditModel {

    @ModelField(showToList = true, nameI18n = {"id", "en:Id"}, infoI18n = {"Id。", "en:Id."})
    public String id;

    @ModelField(
            type = FieldType.bool,
            nameI18n = {"使用系统配置", "en:Use System Config"},
            infoI18n = {"指定是否使用“服务器日志”模块的日志文件相关的配置，若不使用，则可以自行定制。",
                    "en:Specifies whether to use the log file-related configuration of the Server Log module, and if not, you can customize it yourself."})
    public Boolean useSystemConfig = true;

    @ModelField(
            required = true,
            effectiveWhen = "useSystemConfig=false",
            nameI18n = {"文件目录", "en:Base File"},
            infoI18n = {"将日志文件存放到指定的目录下。", "en:Save the log file to the specified directory."})
    public String baseFile = "logs";

    @ModelField(
            type = FieldType.bool,
            effectiveWhen = "useSystemConfig=false",
            nameI18n = {"按天轮转", "en:Rotation by Day"},
            infoI18n = {"将同一天的日志存储到按天命名的单独的文件中。", "en:Store logs for the same day in separate files named by day."})
    public Boolean rotationByDay = true;

    @ModelField(
            type = FieldType.number,
            min = 1,
            effectiveWhen = "useSystemConfig=false",
            nameI18n = {"按大小轮转", "en:Rotation by Size"},
            infoI18n = {"当日志文件大小（单位：MB）达到该阈值时，将切割出一个新的日志文件。", "en:When the log file size (in MB) reaches this threshold, a new log file will be cut out."})
    public Integer rotationBySize = 50;

    @ModelField(
            type = FieldType.number,
            min = 1,
            effectiveWhen = "useSystemConfig=false",
            nameI18n = {"保留文件个数", "en:Keep Files"},
            infoI18n = {"指定在清理日志文件时，须保留文件的个数。", "en:Specifies the number of files to be retained when cleaning up log files."})
    public Integer keepMaxFiles = 100;

    @ModelField(
            type = FieldType.selectCharset,
            effectiveWhen = "useSystemConfig=false",
            nameI18n = {"文件编码", "en:Charset"},
            infoI18n = {"指定日志文件的编码格式。", "en:Specifies the encoding format of the log file."})
    public String charset = "UTF-8";

    @ModelField(
            type = FieldType.bool,
            effectiveWhen = "useSystemConfig=false",
            nameI18n = {"缓冲写入", "en:Buffered"},
            infoI18n = {"是否开启文件缓冲写入功能。开启后，当持续记录的日志内容大小（单位：字节）之和达到缓冲大小时才一次性写入文件，而不是每条日志都立即写入文件（在服务器停止时会全部写入），这通常可以提高日志记录性能。关闭后，每条日志都立即写入文件。",
                    "en:Whether to enable the file buffer write function. After it is enabled, when the sum of the continuously recorded log content size (unit: bytes) reaches the buffer size, the file will be written to the file at one time, instead of writing each log to the file immediately (all of them will be written when the server stops). This often improves logging performance. When closed, each log is written to the file immediately."})
    public boolean buffered = true;

    @ModelField(
            effectiveWhen = "useSystemConfig=false&buffered=true",
            type = FieldType.number,
            min = 1,
            nameI18n = {"缓冲大小", "en:Buffered Size"},
            infoI18n = {"缓冲写入的阈值，当持续记录的日志内容大小（单位：字节）之和达到此值时，一次性写入进文件。"
                    , "en:The threshold for buffered writing. When the sum of the size (unit: bytes) of the log content that is continuously recorded reaches this value, the file will be written to the file at one time."})
    public Integer bufferedSize = 64 * 1024; // 64 KB

    @ModelField(
            type = FieldType.bool,
            effectiveWhen = "useSystemConfig=false",
            nameI18n = {"日志文件压缩", "en:Log File Compression"},
            infoI18n = {"是否开启日志文件压缩功能。开启后，对轮转后的日志文件进行压缩。",
                    "en:If or not to enable the log file compression. When enabled, the log file is compressed after the rotation."})
    public boolean compression = false;

}
