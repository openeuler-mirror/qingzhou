package qingzhou.command.server.config;

import qingzhou.command.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Jvm {
    private final List<Env> envs = new ArrayList<>();
    private String workDir;
    private final List<Arg> args = new ArrayList<>();

    public void prepare(String javaVersion) {
        for (Arg arg : args) {
            arg.prepare(javaVersion);
        }
    }

    /**
     * @return this process builder's environment
     */
    public Map<String, String> environment() {
        return new HashMap<String, String>() {{
            for (Env env : envs) {
                put(env.getName(), env.getValue());
            }
        }};
    }

    /**
     * @return this process builder's program and its arguments
     */
    public List<String> command() {
        List<String> result = new ArrayList<>();
        for (Arg arg : args) {
            if (arg.available()) {
                String argLine = arg.getName();
                if (argLine != null) {
                    argLine = argLine.trim();
                    if (!argLine.isEmpty()) {
                        result.add(argLine);
                    }
                }
            }
        }
        return result;
    }

    public void addArg(Arg arg) {
        args.add(arg);
    }

    public void addEnv(Env env) {
        String value = Utils.stripQuotes(env.getValue());
        env.setValue(value);

        if (env.getName().equals("JAVA_HOME")) {
            if (Utils.isBlank(env.getValue())) {
                return; // JAVA_HOME 为空会导致后面启动报错
            }
        }
        envs.add(env);
    }

    public List<Env> getEnvs() {
        return envs;
    }

    public List<Arg> getArgs() {
        return args;
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }
}
