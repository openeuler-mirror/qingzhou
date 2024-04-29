package qingzhou.console.util;

public class StringCollector {
    private StringBuilder data = new StringBuilder();

    public synchronized void collect(String content) {
        if (data != null) {
            data.append(content);
        }
    }

    public synchronized String destroy() { // 取走数据，即可销毁，避免泄漏
        StringBuilder back = data;
        data = null;
        if (back != null) {
            return back.toString();
        }
        return "";
    }

    @Override
    public String toString() {
        throw ExceptionUtil.unexpectedException("Consider calling the destroy() method.");
    }
}