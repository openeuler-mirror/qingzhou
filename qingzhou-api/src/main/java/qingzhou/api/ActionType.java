package qingzhou.api;

public enum ActionType {
    link,       // 跳转到页面
    monitor,    // 绘图
    files,      // 列出文件
    download,   // 下载
    action_list,// 启停、删除、卸载等操作完毕后跳回 list 页面
    sub_form,
    sub_menu
}
