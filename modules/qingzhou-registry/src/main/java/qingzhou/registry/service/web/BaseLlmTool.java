package qingzhou.registry.service.web;

import java.util.*;

import qingzhou.api.Constants;
import qingzhou.api.Lang;
import qingzhou.llm.ParameterType;
import qingzhou.llm.Tool;
import qingzhou.llm.ToolParameter;

abstract class BaseLlmTool implements Tool {
    private static final ToolParameter langParameter;

    static {
        StringBuilder langParameterDescription = new StringBuilder("指定以哪种国际化语言展示结果，");
        List<String> langList = new ArrayList<>();
        for (Lang lang : Lang.values()) {
            langParameterDescription.append(lang.flag).append(": ").append(lang.info);
            langList.add(lang.flag);
        }
        langParameter = ToolParameter.of(Constants.REQUEST_PARAMETER_NAME_LANG, langParameterDescription.toString(), ParameterType.STRING, false, langList.toArray(new String[0]));
    }

    @Override
    public final String name() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Set<ToolParameter> parameters() {
        return new HashSet<ToolParameter>() {{
            add(langParameter);
        }};
    }

    @Override
    public final Object invoke(Map<String, Object> argsMap) {
        ParameterRetriever retriever = name -> {
            if (argsMap != null) {
                return argsMap.get(name);
            }
            return null;
        };

        WebHandler handler = toolHandler();
        return handler != null ? handler.handle(retriever) : null;
    }

    protected abstract WebHandler toolHandler();
}
