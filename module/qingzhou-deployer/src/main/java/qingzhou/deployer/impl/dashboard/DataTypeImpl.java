package qingzhou.deployer.impl.dashboard;

import qingzhou.api.dashboard.DataType;

public class DataTypeImpl implements DataType {
    private String title;
    private String info;

    @Override
    public DataType title(String title) {
        this.title = title;
        return this;
    }

    @Override
    public DataType info(String info) {
        this.info = info;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public String getInfo() {
        return info;
    }
}
