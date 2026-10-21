package qingzhou.command;

import java.net.URL;

import org.testng.Assert;
import org.testng.annotations.Test;

public class CommandTest {
    private static final String INIT = "qingzhou.command.cmd.init.Init";

    @Test
    public void cmdClass_loadClass_definedByCommandLoader() throws Exception {
        Command.CommandClassLoader loader = new Command.CommandClassLoader(urls());
        Class<?> loaded = loader.loadClass(INIT);
        Assert.assertSame(loaded.getClassLoader(), loader);
    }

    @Test
    public void processorClass_loadClass_delegatedToParent() throws Exception {
        Command.CommandClassLoader loader = new Command.CommandClassLoader(urls());
        Class<?> loaded = loader.loadClass(Processor.class.getName());
        Assert.assertSame(loaded, Processor.class);
    }

    @Test
    public void reloadedCommand_newInstance_castToProcessor() throws Exception {
        Command.CommandClassLoader loader = new Command.CommandClassLoader(urls());
        Processor processor = (Processor) loader.loadClass(INIT).getDeclaredConstructor().newInstance();
        Assert.assertNotNull(processor);
        Assert.assertEquals(processor.classpath().length, 1);
        Assert.assertEquals(processor.classpath()[0], "components/qingzhou-crypto.jar");
    }

    @Test
    public void sameClass_loadTwice_returnsSameClass() throws Exception {
        Command.CommandClassLoader loader = new Command.CommandClassLoader(urls());
        Assert.assertSame(loader.loadClass(INIT), loader.loadClass(INIT));
    }

    private URL[] urls() {
        return new URL[]{Processor.class.getProtectionDomain().getCodeSource().getLocation()};
    }
}