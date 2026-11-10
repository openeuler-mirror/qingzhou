package qingzhou.ai;

import java.util.Collection;
import java.util.Map;

public interface ToolParameterParser {
    Collection<ToolParameter> parse(Map<String, Object> properties);

    class ToolParameter {
        public String name;
        public String description;
        public boolean required;
        public String type;
    }
}
