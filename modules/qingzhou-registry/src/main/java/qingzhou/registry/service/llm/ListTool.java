package qingzhou.registry.service.llm;

import java.util.function.Function;

import org.osgi.service.component.annotations.Component;
import qingzhou.llm.Tool;
import qingzhou.llm.Parameter;

@Component
public class ListTool extends BaseLlmTool implements Tool {
    private final String modelCodeParameter = "modelCode";

    @Override
    public String description() {
        return "";
    }

    @Override
    public Parameter[] parameters() {
        return null;
    }

    @Override
    protected Function<HandlingContext, Object> toolHandler() {
        return null;
    }
}
