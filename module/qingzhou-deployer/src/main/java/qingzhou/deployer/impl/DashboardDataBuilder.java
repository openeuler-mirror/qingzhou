package qingzhou.deployer.impl;

import qingzhou.api.dashboard.Basic;
import qingzhou.api.dashboard.DataBuilder;
import qingzhou.api.dashboard.DataType;
import qingzhou.api.dashboard.Gauge;
import qingzhou.api.dashboard.Histogram;
import qingzhou.api.dashboard.ShareDataset;
import qingzhou.deployer.impl.dashboard.BasicImpl;
import qingzhou.deployer.impl.dashboard.DataTypeImpl;
import qingzhou.deployer.impl.dashboard.GaugeImpl;
import qingzhou.deployer.impl.dashboard.HistogramImpl;
import qingzhou.deployer.impl.dashboard.ShareDatasetImpl;

import java.util.ArrayList;
import java.util.List;

public class DashboardDataBuilder implements DataBuilder {
    private java.util.List<DataType> dataTypes = new ArrayList<>();

    @Override
    public <T> T build(Class<? extends DataType> dataType) {
        if (dataType.equals(Basic.class)) {
            return (T) new BasicImpl();
        }
        if (dataType.equals(Gauge.class)) {
            return (T) new GaugeImpl();
        }
        if (dataType.equals(Histogram.class)) {
            return (T) new HistogramImpl();
        }
        if (dataType.equals(ShareDataset.class)) {
            return (T) new ShareDatasetImpl();
        }

        return null;
    }

    @Override
    public void add(DataType dataType) {
        dataTypes.add(dataType);
    }

    public List<DataType> getDataTypes() {
        return dataTypes;
    }
}
