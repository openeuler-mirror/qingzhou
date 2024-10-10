package qingzhou.api.type;

import java.util.Map;

public interface General extends Add, Delete, Update, List, Show {
    @Override
    default Map<String, String> editData(String id) throws Exception {
        return showData(id);
    }
}
