package qingzhou.framework.pattern;

import java.util.Arrays;

public class FilterPattern {
    public static <T> void doFilter(T context, Filter<T>[] filters) throws Exception {
        if (filters == null) return;

        Process[] processes = Arrays.stream(filters).map(filter -> new Process() {
            @Override
            public void exec() throws Exception {
                boolean doNext = filter.doFilter(context);
                if (!doNext) {
                    throw new InterruptedException(); // 中断链条
                }
            }

            @Override
            public void undo() {
                filter.afterFilter(context);
            }
        }).toArray(Process[]::new);
        ProcessSequence sequence = new ProcessSequence(processes);
        try {
            sequence.exec();
        } catch (InterruptedException e) {
            // 为了中断调用链，这里忽略错误即可
        }
        sequence.undo();
    }

    private static class InterruptedException extends Exception {
    }
}
