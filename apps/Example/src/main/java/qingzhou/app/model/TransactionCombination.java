package qingzhou.app.model;

import qingzhou.api.Model;
import qingzhou.api.ModelField;
import qingzhou.api.type.Combined;
import qingzhou.app.AddModelBase;


@Model(code = TransactionCombination.code, icon = "sitemap",
        menu = qingzhou.app.ExampleMain.MENU_1, order = "5",
        name = {"事务信息", "en:Transaction Info"},
        info = {"事务信息示例，展示组合详情查看。", "en:Transaction information example, showing combination details to view."})
public class TransactionCombination extends AddModelBase implements Combined {
    public static final String code = "this-model-code";
    @ModelField(
            required = true,
            link_action = Combined.ACTION_COMBINED,
            search = true,
            name = {"事务名称", "en:Transaction Name"},
            info = {"该事务的详细名称。", "en:The name of the transaction."})
    public String id;

    @ModelField(
            list = true, search = true,
            name = {"事务状态", "en:Transaction Status"},
            info = {"事务状态。",
                    "en:Transaction Status."})
    public String branchStatus = "";

    @ModelField(
            list = true, search = true,
            name = {"发起方应用", "en:Initiator Application"},
            info = {"发起方应用。",
                    "en:Initiator application."})
    public String initiatorApplication = "";

    @Override
    public void combinedData(String id, DataBuilder dataBuilder) {
        ShowData showData = dataBuilder.buildData(ShowData.class);
        showData.model(TransactionCombination.code).header("事务信息");
        showData.addData("id", "qqqqq11111");
        showData.addData("branchStatus", "回滚成功");
        showData.addData("initiatorApplication", "test-app");
        dataBuilder.addData(showData);

        UmlData umlData = dataBuilder.buildData(UmlData.class);
        umlData.model(TransactionCombination.code).header("事务信息图片");
        umlData.setData("@startuml\n" +
                "Alice -> Bob: test\n" +
                "@enduml");
        dataBuilder.addData(umlData);

        ListData listData = dataBuilder.buildData(ListData.class);
        listData.model(TransactionCombination.code).header("全局事务相关联的分支事务");
        listData.setFields(new String[]{"id", "initiatorApplication", "branchStatus"});
        listData.addFieldValues(new String[]{"transaction11111", "stock-xa", "未知"});
        listData.addFieldValues(new String[]{"transaction22222", "order-xa", "分支事务一阶段失败"});
        dataBuilder.addData(listData);

        ListData listData2 = dataBuilder.buildData(ListData.class);
        listData2.model(User.code).header("全局事务关联日志");
        listData2.setFields(new String[]{"id", "phoneNumber", "position"});
        listData2.addFieldValues(new String[]{"2024-10-27 18:00:00", "transaction11111", "第1条日志"});
        listData2.addFieldValues(new String[]{"2024-10-28 09:00:00", "transaction22222", "第2条日志"});
        dataBuilder.addData(listData2);
    }

    @Override
    public boolean showOrderNumber() {
        return true;
    }

}
