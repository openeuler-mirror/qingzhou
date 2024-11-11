package qingzhou.deployer;

import qingzhou.api.Item;
import qingzhou.api.type.Echo;
import qingzhou.registry.ItemInfo;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EchoDataBuilder extends ResponseData implements Echo.DataBuilder {
    public Map<String, String> data = new LinkedHashMap<>();
    public List<EchoData> options = new LinkedList<>();

    @Override
    public void addData(String field, String value) {
        data.put(field, value);
    }

    @Override
    public void addData(String field, String value, Item[] options) {
        EchoData echoData = new EchoData();
        echoData.field = field;
        echoData.value = value;
        for (Item option : options) {
            echoData.options.add(new ItemInfo(option));
        }
        this.options.add(echoData);
    }

    public static class EchoData {
        public String field;
        public String value;
        public List<ItemInfo> options = new LinkedList<>();
    }
}
