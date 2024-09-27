package qingzhou.api.type;

import qingzhou.api.Groups;

import java.util.Map;

public interface General extends Add, Delete, Update, List, Show, Grouped {
    @Override
    default Map<String, String> editData(String id) throws Exception {
        return showData(id);
    }

    @Override
    default Groups groups() {
        return null;
    }
}
