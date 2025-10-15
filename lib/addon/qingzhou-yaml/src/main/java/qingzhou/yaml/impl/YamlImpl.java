package qingzhou.yaml.impl;


import org.yaml.snakeyaml.DumperOptions;
import qingzhou.yaml.Yaml;

public class YamlImpl implements Yaml {
    @Override
    public Object fromYaml(String yaml) {
        return yaml().load(yaml);
    }

    @Override
    public String toYaml(Object data) {
        return yaml().dump(data);
    }

    private org.yaml.snakeyaml.Yaml yaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new org.yaml.snakeyaml.Yaml(options);
    }
}
