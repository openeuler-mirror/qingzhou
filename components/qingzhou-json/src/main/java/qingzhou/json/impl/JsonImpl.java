package qingzhou.json.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import qingzhou.json.Json;

@Component
public class JsonImpl implements Json {
    private ObjectMapper objectMapper;

    @Activate
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // transient 表示本进程内部状态（如 registry 写入的存活时间/密钥版本），不随报文收发
        objectMapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);
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
