package qingzhou.engine.util.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProcessPattern implements Process {
    private final List<Process> processList = new ArrayList<>();
    private int index = -1;

    public ProcessPattern(Process... processes) {
        this(Arrays.asList(processes));
    }

    public ProcessPattern(List<Process> processes) {
        if (processes == null) throw new IllegalArgumentException();
        for (Process process : processes) {
            if (process == null) throw new IllegalArgumentException();
            processList.add(process);
        }
    }

    @Override
    public void run() throws Throwable {
        for (Process process : processList) {
            process.run();
            index++;
        }
    }

    @Override
    public void completed() {
        for (int i = index; i >= 0; i--) {
            processList.get(i).completed();
        }
    }
}
