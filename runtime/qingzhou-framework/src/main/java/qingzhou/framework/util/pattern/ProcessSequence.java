package qingzhou.framework.util.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProcessSequence implements Process {
    private final List<Process> processList;
    private int index = -1;

    public ProcessSequence(Process... processes) {
        processList = new ArrayList<>(Arrays.asList(processes));
    }

    @Override
    public void exec() throws Exception {
        for (Process process : processList) {
            process.exec();
            index++;
        }
    }

    @Override
    public void undo() {
        for (int i = index; i >= 0; i--) {
            processList.get(i).undo();
        }
    }
}
