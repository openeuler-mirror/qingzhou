package qingzhou.yaml.impl;


import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import qingzhou.yaml.YamlParser;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class YamlParserImpl implements YamlParser {
    private final Yaml yamlReader;
    private final Yaml yamlWriter;

    public YamlParserImpl() {
        this.yamlReader = new Yaml();
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yamlWriter = new Yaml(options);
    }

    @Override
    public Map<String, Object> readYaml(String filePath) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath))) {
            return yamlReader.load(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void writeYaml(Map<String, Object> data, String filePath) {
        try (OutputStream outputStream = Files.newOutputStream(Paths.get(filePath))) {
            yamlWriter.dump(data, new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
