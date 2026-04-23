package qingzhou.json.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import qingzhou.json.Json;

@Component
public class JsonImpl implements Json {
    private com.fasterxml.jackson.databind.ObjectMapper MAPPER;

    @Activate
    public void init() {
        MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();
        MAPPER.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        MAPPER.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String toJson(Object src) throws Exception {
        return MAPPER.writeValueAsString(src);
    }

    @Override
    public <T> T fromJson(String json, Class<T> classOfT) throws Exception {
        return MAPPER.readValue(json, classOfT);
    }
}
