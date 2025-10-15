package qingzhou.yaml;

import qingzhou.engine.Service;

@Service(name = "YAML Processor", description = "YAML 1.1 parser and emitter for Java.")
public interface Yaml {
    Object fromYaml(String yaml);

    String toYaml(Object data);
}
