package qingzhou.app.model;

import qingzhou.api.Model;
import qingzhou.api.ModelBase;
import qingzhou.app.ExampleMain;

import java.util.Random;

@Model(code = "dashboard", icon = "dashboard", menu = ExampleMain.MENU_1,
        entrance = Dashboard.ACTION_DASHBOARD,
        name = {"概览", "en:Dashboard"},
        info = {"监视信息概览。", "en:Overview of monitoring information."})

public class Dashboard extends ModelBase implements qingzhou.api.type.Dashboard {
    @Override
    public void dashboardData(String id, DataBuilder dataBuilder) {
        Random random = new Random();
        Basic basic = dataBuilder.build(Basic.class);
        basic.info("测试").title("基础数据");
        String[] units = new String[]{"GB", "MB", "KB"};
        for (int i = 0; i < 6; i++) {
            basic.put("key" + i, random.nextInt(10) + units[random.nextInt(2)]);
        }

        dataBuilder.add(basic);
        dataBuilder.build(Basic.class);

        for (int count = 0; count < 5; count++) {
            Gauge gauge = dataBuilder.build(Gauge.class);
            gauge.info("内存使用情况" + count).title("内存" + count);
            gauge.maxKey("max").unit("GB").statusKey("used").fields(new String[]{"name", "ip", "used", "max"});
            for (int i = 0; i < 4; i++) {
                int used = random.nextInt(10);
                int max = random.nextInt(10);
                if (max < used) {
                    max = used;
                }
                gauge.addData(new String[]{"实例" + i, "127.0.0.1", String.valueOf(used), String.valueOf(max)});
            }
            dataBuilder.add(gauge);
        }

        for (int count = 0; count < 2; count++) {
            Histogram histogram = dataBuilder.build(Histogram.class);
            histogram.info("网络使用情况" + count).title("网络" + count);
            histogram.unit("MB");
            histogram.maxKey("max");
            histogram.statusKey("used");
            histogram.fields(new String[]{"name", "ip", "used", "max"});
            for (int i = 0; i < 4; i++) {
                int used = random.nextInt(100);
                int max = random.nextInt(100);
                if (max < used) {
                    max = used;
                }
                histogram.addData(new String[]{"实例" + i, "127.0.0.1", String.valueOf(used), String.valueOf(max)});
            }
            dataBuilder.add(histogram);
        }

        Histogram histogram = dataBuilder.build(Histogram.class);
        histogram.info("网络使用情况" + 3).title("网络" + 3);
        histogram.unit("MB");
        histogram.statusKey("used");
        histogram.fields(new String[]{"name", "ip", "used"});
        for (int i = 0; i < 4; i++) {
            histogram.addData(new String[]{"实例" + i, "127.0.0.1", String.valueOf(random.nextInt(100))});
        }
        dataBuilder.add(histogram);

        for (int count = 0; count < 2; count++) {
            ShareDataset shareDataset = dataBuilder.build(ShareDataset.class);
            shareDataset.info("分享数据集" + count).title("数据集" + count);
            for (int i = 0; i < 4; i++) {
                shareDataset.put("key" + i, String.valueOf(random.nextInt(10)));
            }
            dataBuilder.add(shareDataset);
        }
    }

}
