package qingzhou.command;

import java.util.*;

public class Admin extends CommandLineProcessor { // 由 Launcher 来调用

    @Override
    public String name() {
        return "admin";
    }

    @Override
    public String info() {
        return "The entry point for the execution of all commands";
    }

    @Override
    public void doCommandLine(String[] args) throws Exception {
        if (showHelp(args)) {
            return;
        }

        if (args == null || args.length < 1) {
            System.out.println("wrong command args. Type [ --help ] for help");
            return;
        }

        List<CommandLineProcessor> found = new ArrayList<>();
        for (CommandLineProcessor processor : listCommandsTemp()) {
            AcceptStatus acceptStatus = accept(processor, args[0]);
            if (acceptStatus == AcceptStatus.none) {
                continue;
            }

            if (acceptStatus == AcceptStatus.full) {
                found.clear();
                found.add(processor);
                break;
            }

            if (acceptStatus == AcceptStatus.startsWith) {
                found.add(processor);// 模糊匹配，例如 startsWith
            }
        }
        if (found.isEmpty()) {
            System.out.println("command not found: " + args[0]);
        } else if (found.size() == 1) {
            CommandLineProcessor processor = found.get(0);
            String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);
            processor.doCommandLine(commandArgs);
        } else {
            Object[] names = found.stream().map(CommandLineProcessor::name).toArray();
            System.out.println("Find more than one command: " + Arrays.toString(names));
        }
    }

    private List<CommandLineProcessor> listCommandsTemp() {
        List<CommandLineProcessor> processors = new ArrayList<>();
        ServiceLoader.load(CommandLineProcessor.class, CommandLineProcessor.class.getClassLoader()).forEach(processors::add);
        processors.sort(Comparator.comparing(CommandLineProcessor::name));
        return processors;
    }

    private boolean showHelp(String[] args) {
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
                StringBuilder msg = new StringBuilder("usage: admin <command> [args]");
                msg.append(System.lineSeparator());
                msg.append("Supported commands: ");
                for (CommandLineProcessor processor : listCommandsTemp()) {
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

    private AcceptStatus accept(CommandLineProcessor processor, String cmd) {
        while (cmd.startsWith("-")) {
            cmd = cmd.substring(1);
        }
        boolean equals = Objects.equals(processor.name(), cmd);
        if (equals) {
            return AcceptStatus.full;
        }
        if (processor.name().startsWith(cmd)) {
            return AcceptStatus.startsWith;
        }
        return AcceptStatus.none;
    }

    private enum AcceptStatus {
        full, none, startsWith
    }
}
