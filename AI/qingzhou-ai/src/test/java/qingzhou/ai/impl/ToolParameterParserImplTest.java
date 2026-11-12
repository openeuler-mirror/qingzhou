package qingzhou.ai.impl;

import java.util.*;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.ai.ToolParameterParser.ToolParameter;
import qingzhou.ai.ToolService;

public class ToolParameterParserImplTest {

    private static List<ToolParameter> parse(Map<String, Object> properties) {
        return new ArrayList<>(new ToolParameterParserImpl().parse(properties));
    }

    private static Map<String, Object> properties(String... keyValues) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            properties.put(keyValues[i], keyValues[i + 1]);
        }
        return properties;
    }

    @Test
    public void dataSingleParameterAllFields_parse_allFieldsBound() {
        List<ToolParameter> parameters = parse(properties(
                ToolService.PARAMETER_NAME + ".1", "instanceId",
                ToolService.PARAMETER_DESCRIPTION + ".1", "实例 ID",
                ToolService.PARAMETER_REQUIRED + ".1", "true",
                ToolService.PARAMETER_TYPE + ".1", "integer"));
        Assert.assertEquals(parameters.size(), 1);
        ToolParameter parameter = parameters.get(0);
        Assert.assertEquals(parameter.name, "instanceId");
        Assert.assertEquals(parameter.description, "实例 ID");
        Assert.assertTrue(parameter.required);
        Assert.assertEquals(parameter.type, "integer");
    }

    @Test
    public void dataMultipleParameters_parse_groupedBySuffix() {
        List<ToolParameter> parameters = parse(properties(
                ToolService.PARAMETER_NAME + ".1", "app",
                ToolService.PARAMETER_NAME + ".2", "page",
                ToolService.PARAMETER_DESCRIPTION + ".2", "页码",
                ToolService.PARAMETER_TYPE + ".2", "integer"));
        Assert.assertEquals(parameters.size(), 2);
        Assert.assertEquals(parameters.get(0).name, "app");
        Assert.assertNull(parameters.get(0).description);
        Assert.assertEquals(parameters.get(0).type, "string");
        Assert.assertEquals(parameters.get(1).name, "page");
        Assert.assertEquals(parameters.get(1).description, "页码");
        Assert.assertEquals(parameters.get(1).type, "integer");
    }

    @Test
    public void dataTypeAbsent_parse_defaultStringType() {
        List<ToolParameter> parameters = parse(properties(
                ToolService.PARAMETER_NAME + ".1", "code"));
        Assert.assertEquals(parameters.get(0).type, "string");
    }

    @Test
    public void dataRequiredAbsent_parse_notRequired() {
        List<ToolParameter> parameters = parse(properties(
                ToolService.PARAMETER_NAME + ".1", "code"));
        Assert.assertTrue(parameters.get(0).required);
    }

    @Test
    public void dataToolLevelProperties_parse_ignored() {
        List<ToolParameter> parameters = parse(properties(
                ToolService.TOOL_NAME, "tool",
                ToolService.TOOL_DESCRIPTION, "desc"));
        Assert.assertTrue(parameters.isEmpty());
    }

    @Test
    public void dataNullRequired_parse_notRequired() {
        List<ToolParameter> parameters = parse(properties(
                ToolService.PARAMETER_NAME + ".1", "code",
                ToolService.PARAMETER_REQUIRED + ".1", null));
        Assert.assertTrue(parameters.get(0).required);
    }

    @Test
    public void dataNonDeclaredParameterKeys_parse_ignored() {
        List<ToolParameter> parameters = parse(properties(
                "component.name", "qingzhou.ai",
                ToolService.PARAMETER_NAME, "orphan",
                ToolService.PARAMETER_NAME + "_EXTRA.1", "x"));
        Assert.assertTrue(parameters.isEmpty());
    }
}
