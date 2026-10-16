package qingzhou.command;

import java.io.File;
import java.util.*;

public class Command {
    private final File libDir;

    public Command(File libDir) {
        this.libDir = libDir;
    }

    public void doCommand(String[] args) throws Exception {
        List<Processor> commands = listCommands();

        if (showHelp(args, commands)) {
            return;
        }

        if (args.length < 1) {
            System.out.println("wrong command args. Type [ --help ] for help");
            return;
        }

        List<Processor> found = new ArrayList<>();
        for (Processor processor : commands) {
            AcceptStatus acceptStatus = accept(processor, args[0]);
            if (acceptStatus == AcceptStatus.NONE) {
                continue;
            }

            if (acceptStatus == AcceptStatus.FULL) {
                found.clear();
                found.add(processor);
                break;
            }

            if (acceptStatus == AcceptStatus.STARTS_WITH) {
                found.add(processor);
            }
        }
        if (found.isEmpty()) {
            System.out.println("command not found: " + args[0]);
        } else if (found.size() == 1) {
            Processor processor = found.get(0);
            String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);
            processor.doCommandLine(commandArgs);
        } else {
            String[] names = found.stream().map(Processor::name).toArray(String[]::new);
            System.out.println("Find more than one command: " + Arrays.toString(names));
        }
    }

    private List<Processor> listCommands() {
        List<Processor> processors = new ArrayList<>();
        ServiceLoader.load(Processor.class, Processor.class.getClassLoader()).forEach(e -> {
            e.setLibDir(libDir);
            processors.add(e);
        });
        processors.sort(Comparator.comparing(Processor::name));
        return processors;
    }

    private boolean showHelp(String[] args, List<Processor> commands) {
        if (args == null || args.length == 0 || args.length == 1) {
            boolean found = (args == null || args.length == 0);
            if (!found) {
                String[] helpFlag = {"-h", "--help", "help"};
                for (String h : helpFlag) {
                    if (args[0].equalsIgnoreCase(h)) {
                        found = true;
                        break;
                    }
                }
            }
            if (found) {
                StringBuilder msg = new StringBuilder("Supported commands: ");
                for (Processor processor : commands) {
                    msg.append(System.lineSeparator());
                    msg.append("\t").append("<").append(processor.name()).append(">");
                    String[] supportedArgs = processor.supportedArgs();
                    if (supportedArgs != null && supportedArgs.length > 0) {
                        msg.append(" ").append(Arrays.toString(supportedArgs));
                    }

                    msg.append(System.lineSeparator());
                    msg.append("\t\t").append(processor.info());
                    msg.append(System.lineSeparator());

                }
                System.out.println(msg);
                return true;
            }
        }
        return false;
    }

    private AcceptStatus accept(Processor processor, String cmd) {
        while (cmd.startsWith("-")) {
            cmd = cmd.substring(1);
        }
        if (cmd.isEmpty()) return AcceptStatus.NONE; // 纯 "-" 输入剥完为空串：startsWith("") 会模糊命中全部命令
        boolean equals = Objects.equals(processor.name(), cmd);
        if (equals) {
            return AcceptStatus.FULL;
        }
        if (processor.name().startsWith(cmd)) {
            return AcceptStatus.STARTS_WITH;
        }
        return AcceptStatus.NONE;
    }

    private enum AcceptStatus {
        FULL, NONE, STARTS_WITH
    }
}
