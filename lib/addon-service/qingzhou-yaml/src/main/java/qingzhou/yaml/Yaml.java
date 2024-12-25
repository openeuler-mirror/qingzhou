package qingzhou.yaml;

import qingzhou.engine.Service;

@Service(name = "YAML Parser", description = "Provides parser tools for YAML or YML.")
public interface Yaml {
    Object fromYaml(String yaml);

    String toYaml(Object data);
}
