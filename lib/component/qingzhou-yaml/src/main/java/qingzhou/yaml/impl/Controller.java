package qingzhou.yaml.impl;

import qingzhou.engine.Module;
import qingzhou.engine.ModuleActivator;
import qingzhou.engine.ModuleContext;
import qingzhou.yaml.YamlParser;

@Module
public class Controller implements ModuleActivator {
    @Override
    public void start(ModuleContext context) {
        context.registerService(YamlParser.class, new YamlParserImpl());
    }
}
