package qingzhou.registry.service.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import qingzhou.api.Constants;
import qingzhou.api.Lang;
import qingzhou.llm.ParameterType;
import qingzhou.llm.Tool;
import qingzhou.llm.ToolParameter;

public abstract class BaseLlmTool implements Tool {
    protected static final ToolParameter langParameter;
//    protected static final ToolParameter instanceParameter;

    static {
        StringBuilder langParameterDescription = new StringBuilder("指定以哪种国际化语言展示结果，");
        List<String> langList = new ArrayList<>();
        for (Lang lang : Lang.values()) {
            langParameterDescription.append(lang.flag).append(": ").append(lang.info);
            langList.add(lang.flag);
        }
        langParameter = ToolParameter.of(Constants.REQUEST_PARAMETER_NAME_LANG, langParameterDescription.toString(), ParameterType.STRING, false, langList.toArray(new String[0]));

//        instanceParameter=ToolParameter.of();
    }

    @Override
    public final String name() {
        return this.getClass().getSimpleName();
    }

    @Override
    public final Object invoke(Map<String, Object> argsMap) {
        HandlingContext context = name -> {
            if (argsMap != null) {
                Object val = argsMap.get(name);
                return val != null ? String.valueOf(val) : null;
            }
            return null;
        };

        Function<HandlingContext, Object> handler = toolHandler();
        return handler != null ? handler.apply(context) : null;
    }

    protected abstract Function<HandlingContext, Object> toolHandler();
}
