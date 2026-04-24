package qingzhou.chat;

import qingzhou.http.server.HttpResponse;

import java.util.concurrent.CountDownLatch;

public class SseEmitter {

    private final HttpResponse writer;
    private final CountDownLatch countDownLatch;

    public SseEmitter(HttpResponse res, CountDownLatch countDownLatch) {
        this.writer = res;
        this.countDownLatch = countDownLatch;
    }

    public void complete() {
        try {
            sendEvent(SseEventType.RUN_FINISHED, "{}");
        } finally {
            countDownLatch.countDown();
        }
    }

    /**
     * 发送一个 SSE 事件
     * @param event 事件类型（event: 行）
     * @param data  事件数据（data: 行），必须是合法 JSON 字符串
     * @return true 发送成功，false 客户端已断开
     */
    public boolean sendEvent(SseEventType event, String data) {
        write("event: " + event.name() + "\n");
        write("data: " + data + "\n");
        write("\n");  // 空行表示事件结束
        return true;
    }

    public boolean completeWithError(String error) {
        try {
            return sendEvent(SseEventType.RUN_ERROR, error);
        } finally {
            countDownLatch.countDown();
        }
    }

    private void write(String data) {
        writer.sendResponse(data);
    }
}
