package qingzhou.yaml;

import qingzhou.engine.Service;

import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;

@Service(name = "YAML Parser", description = "Provides parser tools for YAML or YML.")
public interface YamlParser {

    Map<String, Object> readYaml(String yamlFilePath);

    void writeYaml(Map<String, Object> data, String filePath);

}
