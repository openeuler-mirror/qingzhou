package qingzhou.ai.impl;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import qingzhou.ai.ToolParameterParser;
import qingzhou.ai.ToolService;

@Component
public class ToolParameterParserImpl implements ToolParameterParser {
    @Override
    public Collection<ToolParameter> parse(Map<String, Object> properties) {
        Map<String, ToolParameter> params = new LinkedHashMap<>();

        properties.forEach((key, value) ->
                Stream.of(ToolService.PARAMETER_NAME, ToolService.PARAMETER_DESCRIPTION, ToolService.PARAMETER_REQUIRED, ToolService.PARAMETER_TYPE)
                        .forEach(flag -> {
                            if (!key.startsWith(flag)) return;

                            String parameterGroup = "";
                            int i = key.indexOf(".");
                            if (i != -1) {
                                parameterGroup = key.substring(i);
                            }

                            ToolParameter toolParameter = params.computeIfAbsent(parameterGroup, s -> new ToolParameter());
                            switch (flag) {
                                case ToolService.PARAMETER_NAME:
                                    toolParameter.name = Objects.toString(value, null);
                                    break;
                                case ToolService.PARAMETER_DESCRIPTION:
                                    toolParameter.description = Objects.toString(value, null);
                                    break;
                                case ToolService.PARAMETER_REQUIRED:
                                    toolParameter.required = value == null || Boolean.parseBoolean(Objects.toString(value));
                                    break;
                                case ToolService.PARAMETER_TYPE:
                                    String type = Objects.toString(value, "");
                                    if (!type.isEmpty()) {
                                        toolParameter.type = type;
                                    }
                                    break;
                            }
                        })
        );

        return params.values();
    }
}
