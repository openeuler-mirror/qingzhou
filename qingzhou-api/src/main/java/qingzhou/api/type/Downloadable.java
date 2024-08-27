package qingzhou.api.type;

import java.io.File;

/**
 * 定义了与下载相关的操作。
 */
public interface Downloadable {
    File downloadData(String id);
}