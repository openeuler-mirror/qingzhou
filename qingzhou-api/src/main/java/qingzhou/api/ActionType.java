package qingzhou.api;

public enum ActionType {
    link,       // 跳转到页面
    monitor,    // 绘图
    files,      // 列出文件
    download,   // 下载
    delete,     // 删除、卸载等操作
    StartStop, PopLayer, ViewHtml, SubTab, NewTab // todo：根据类型来调用 SuperAction 而不能再是 code = XX
}
