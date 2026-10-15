package qingzhou.command.cmd;

import java.io.Console;
import java.util.Arrays;
import java.util.List;

import qingzhou.command.Processor;

public class Wizard extends Processor {
    private final List<Processor> commands = Arrays.asList(
            new InitInstance(), new CipherKey(), new PairKey(), new TotpKey(),
            new AuthPassword(), new CipherPassword(), new Keystore());

    @Override
    public String name() {
        return "wizard";
    }

    @Override
    public String info() {
        return "Interactive wizard that guides through key/password/keystore generation step by step.";
    }

    @Override
    public void doCommandLine(String[] args) {
        Console console = System.console();
        if (console == null) {
            log("no interactive terminal found, run this command in a terminal");
            return;
        }

        while (true) {
            log("");
            log("Select an action:");
            for (int i = 0; i < commands.size(); i++) {
                Processor command = commands.get(i);
                log("  " + (i + 1) + ". " + command.name() + " - " + command.info());
            }
            log("  0. exit");

            String choice = console.readLine("number: ");
            if (choice == null || choice.trim().isEmpty() || choice.trim().equals("0")) return;

            Processor selected;
            try {
                selected = commands.get(Integer.parseInt(choice.trim()) - 1);
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                log("invalid choice: " + choice.trim());
                continue;
            }

            try {
                selected.setLibDir(getLibDir());
                selected.doCommandLine(commandArgs(selected, console));
            } catch (Exception e) {
                log("failed: " + e.getMessage());
            }
        }
    }

    // instance 级命令先询问实例名；密码类命令传空参数以走交互式不回显读取
    private String[] commandArgs(Processor command, Console console) {
        if (command instanceof InitInstance || command instanceof Keystore) {
            String instance = console.readLine("instance name [default]: ");
            return new String[]{instance == null || instance.trim().isEmpty() ? "default" : instance.trim()};
        }
        return new String[0];
    }
}
