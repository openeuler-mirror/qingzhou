package qingzhou.json.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import qingzhou.json.Json;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class JsonImpl implements Json {
    private ObjectMapper objectMapper;

    @Activate
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 输入边界：拒绝尾随 JSON 值与重复键，避免 {...}{...} 被静默截断造成解析歧义
        objectMapper.configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true);
        objectMapper.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);
        // transient 表示本进程内部状态（如 registry 写入的存活时间/密钥版本），不随报文收发
        objectMapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);
        // 元数据集合声明为 Set 接口时 Jackson 默认实例化 HashSet，迭代顺序改由 code 哈希决定，
        // 会把 annotation.json 里的字段声明顺序打乱（前端按该顺序渲染表单）。
        SimpleAbstractTypeResolver types = new SimpleAbstractTypeResolver();
        types.addMapping(Set.class, LinkedHashSet.class);
        SimpleModule module = new SimpleModule();
        module.setAbstractTypes(types);
        objectMapper.registerModule(module);
    }

    @Override
    public String toJson(Object src) throws Exception {
        return objectMapper.writeValueAsString(src);
    }

    @Override
    public <T> T fromJson(String json, Class<T> classOfT) throws Exception {
        return objectMapper.readValue(json, classOfT);
    }
}
