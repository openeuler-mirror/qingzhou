package qingzhou.command.server.config;

import java.util.List;

public class Jvm {
    private List<Env> env;
    private List<Arg> arg;

    public List<Env> getEnv() {
        return env;
    }

    public void setEnv(List<Env> env) {
        this.env = env;
    }

    public List<Arg> getArg() {
        return arg;
    }

    public void setArg(List<Arg> arg) {
        this.arg = arg;
    }
}
