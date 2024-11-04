package qingzhou.api.type;

import qingzhou.api.dashboard.DataBuilder;

public interface Dashboard {
    String ACTION_DASHBOARD = "dashboard";

    void dashboardData(String id, DataBuilder builder);
}
