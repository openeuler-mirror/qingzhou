package qingzhou.api.type;

import qingzhou.api.QingzhouModel;
import qingzhou.api.Request;

/**
 * 选择器接口。
 * 用户实现此接口以支持前端选择器的"激活"和"取消激活"操作。
 */
public interface Selector extends QingzhouModel {
    String ACTION_CODE_ACTIVATE = "activate";
    String ACTION_CODE_DEACTIVATE = "deactivate";
    String ACTION_CODE_ACTIVE = "active";

    /**
     * 激活指定的选项。
     * @param request 请求对象，可通过 request.getId() 获取要激活的 ID
     */
    void activate(Request request) throws Exception;

    /**
     * 取消激活当前选项。
     * @param request 请求对象
     */
    void deactivate(Request request) throws Exception;

    /**
     * 返回当前激活的选项 ID，无激活项返回 null。
     */
    default String activeId() {
        return null;
    }
}
