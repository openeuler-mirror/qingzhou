package qingzhou.api;

public enum ActionType {
    link,       // 跳转到页面
    monitor,    // 绘图
    files,      // 列出文件
    download,   // 下载
    StartStop, PopLayer, ViewHtml, SubTab // todo：根据类型来调用 SuperAction 而不能再是 code = XX
}
