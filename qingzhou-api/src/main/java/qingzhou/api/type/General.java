package qingzhou.api.type;

import java.util.Map;

import qingzhou.api.Groups;

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
