package qingzhou.api;

public enum ActionType {
    link,       // 跳转到页面
    download,      // 列出文件
    upload, //上传
    action_list,// 启停、删除、卸载等操作完毕后跳回 list 页面
    sub_form,
    sub_menu,
    qr         // 点击可显示一个二维码
}
