package qingzhou.app.model;

import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.app.ExampleMain;

import java.util.Random;

@Model(code = "dashboard", icon = "dashboard", menu = ExampleMain.MENU_1,
        order = "0",
        entrance = Dashboard.ACTION_DASHBOARD,
        name = {"概览", "en:Dashboard"},
        info = {"监视信息概览。", "en:Overview of monitoring information."})

public class Dashboard extends ModelBase implements qingzhou.api.type.Dashboard {
    @Override
    public void dashboardData(String id, DataBuilder dataBuilder) {
        Random random = new Random();
        // 基础数据
        Basic basic = dataBuilder.buildData(Basic.class);
        basic.title("基础数据");
        String[] units = new String[]{"GB", "MB", "KB"};
        for (int i = 0; i < 6; i++) {
            basic.addData("key" + i, random.nextInt(10) + units[random.nextInt(2)]);
        }
        Basic[] basics = new Basic[]{basic};
        dataBuilder.addData(basics);

        // 仪表板
        Gauge[] gauges = new Gauge[5];
        for (int count = 0; count < 5; count++) {
            Gauge gauge = dataBuilder.buildData(Gauge.class);
            gauge.info("内存使用情况" + count).title("内存" + count);
            gauge.maxKey("max").unit("GB").usedKey("used").fields(new String[]{"name", "ip", "used", "max"});
            for (int i = 0; i < 4; i++) {
                int used = random.nextInt(10);
                int max = random.nextInt(10);
                if (max < used) {
                    max = used;
                }
                gauge.addData(new String[]{"实例" + i, "127.0.0.1", String.valueOf(used), String.valueOf(max)});
            }
            gauges[count] = gauge;
        }
        dataBuilder.addData(gauges);

        // 柱状图
        Histogram[] histograms = new Histogram[2];
        for (int count = 0; count < 2; count++) {
            Histogram histogram = dataBuilder.buildData(Histogram.class);
            histogram.info("网络使用情况" + count).title("网络" + count);
            histogram.unit("MB");
            histogram.maxKey("max");
            histogram.usedKey("used");
            histogram.fields(new String[]{"name", "ip", "used", "max"});
            for (int i = 0; i < 4; i++) {
                int used = random.nextInt(100);
                int max = random.nextInt(100);
                if (max < used) {
                    max = used;
                }
                histogram.addData(new String[]{"实例" + i, "127.0.0.1", String.valueOf(used), String.valueOf(max)});
            }
            histograms[count] = histogram;
        }
        dataBuilder.addData(histograms);

        // 仪表盘
        Gauge[] gauges1 = new Gauge[2];
        for (int count = 0; count < 2; count++) {
            Gauge gauge = dataBuilder.buildData(Gauge.class);
            gauge.info("硬盘使用情况" + count).title("硬盘" + count);
            gauge.maxKey("max").unit("GB").usedKey("used").fields(new String[]{"name", "ip", "used", "max"});
            for (int i = 0; i < 4; i++) {
                int used = random.nextInt(10);
                int max = random.nextInt(10);
                if (max < used) {
                    max = used;
                }
                gauge.addData(new String[]{"实例" + i, "127.0.0.1", String.valueOf(used), String.valueOf(max)});
            }
            gauges1[count] = gauge;
        }
        dataBuilder.addData(gauges1);

        // 柱状图
        Histogram histogram = dataBuilder.buildData(Histogram.class);
        histogram.info("网络使用情况" + 3).title("网络" + 3);
        histogram.unit("MB");
        histogram.usedKey("used");
        histogram.fields(new String[]{"name", "ip", "used"});
        for (int i = 0; i < 4; i++) {
            histogram.addData(new String[]{"实例" + i, "127.0.0.1", String.valueOf(random.nextInt(100))});
        }
        Histogram[] histograms1 = new Histogram[]{histogram};
        dataBuilder.addData(histograms1);

        // 基础数据
        Basic basic1 = dataBuilder.buildData(Basic.class);
        basic1.title("销售数据");
        basic1.addData("销售额", random.nextInt(100) + "（万元）");
        basic1.addData("销售量", random.nextInt(100) + "（套）");
        basic1.addData("成交率", random.nextInt(100) + " %");
        Basic[] basics1 = new Basic[]{basic1};
        dataBuilder.addData(basics1);

        // 数据集
        ShareDataset[] shareDatasets = new ShareDataset[2];
        for (int count = 0; count < 2; count++) {
            ShareDataset shareDataset = dataBuilder.buildData(ShareDataset.class);
            shareDataset.title("数据集" + count);
            for (int i = 0; i < 4; i++) {
                shareDataset.addData("key" + i, String.valueOf(random.nextInt(10)));
            }
            shareDatasets[count] = shareDataset;
        }
        dataBuilder.addData(shareDatasets);

        // 热力图
        MatrixHeatmap matrixHeatmap = dataBuilder.buildData(MatrixHeatmap.class);
        matrixHeatmap.title("应用实例部署图");
        matrixHeatmap.showValue(false);
        matrixHeatmap.xAxisName("实例");
        matrixHeatmap.yAxisName("应用");
        for (int i = 0; i < 50; i++) {
            matrixHeatmap.addData("实例" + random.nextInt(10), "应用" + random.nextInt(8), random.nextInt(10));
        }
        dataBuilder.addData(new MatrixHeatmap[]{matrixHeatmap});

        // 折线图
        LineChart lineChart = dataBuilder.buildData(LineChart.class);
        lineChart.title("内存使用情况");
        lineChart.yAxis("内存").xAxis("时间").unit("MB");
        lineChart.addData("Web", String.valueOf(random.nextInt(500)));
        lineChart.addData("Tomcat", String.valueOf(random.nextInt(500)));
        dataBuilder.addData(new LineChart[]{lineChart});
    }

    @Override
    public int period() {
        return 3000;
    }
}
