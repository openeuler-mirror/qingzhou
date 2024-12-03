package qingzhou.config;

public class Core {
    private Console console;
    private Deployer deployer;
    private Registry registry;
    private Agent agent;

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public Console getConsole() {
        return console;
    }

    public void setConsole(Console console) {
        this.console = console;
    }

    public Deployer getDeployer() {
        return deployer;
    }

    public void setDeployer(Deployer deployer) {
        this.deployer = deployer;
    }
}
