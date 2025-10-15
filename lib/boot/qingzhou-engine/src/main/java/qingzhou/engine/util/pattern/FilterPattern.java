package qingzhou.engine.util.pattern;

import java.util.ArrayList;
import java.util.List;

public class FilterPattern {
    private final CompositeProcess compositeProcess;

    public <T> FilterPattern(Filter<T>[] filters, T context) {
        if (filters == null) throw new IllegalArgumentException();
        List<Process> processList = new ArrayList<>();
        for (Filter<T> filter : filters) {
            if (filter == null) throw new IllegalArgumentException();
            processList.add(new Process() {
                @Override
                public void run() throws Throwable {
                    boolean doNext = filter.doFilter(context);
                    if (!doNext) {
                        throw new InternalInterruptedSignal(); // 中断链条
                    }
                }

                @Override
                public void completed() {
                    filter.postFilter(context);
                }
            });
        }
        compositeProcess = new CompositeProcess(processList);
    }

    public void doFilter() throws Throwable {
        try {
            compositeProcess.run();
        } catch (InternalInterruptedSignal ignored) {
        } finally {
            compositeProcess.completed();
        }
    }

    private static class InternalInterruptedSignal extends Throwable {
    }
}
