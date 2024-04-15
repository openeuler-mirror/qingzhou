package qingzhou.registry;

public class ModelInfo {
    private String name;
    private String icon;
    private String[] nameI18n;
    private String[] infoI18n;
    private String entryAction;
    private boolean showToMenu;
    private String menuName;
    private int menuOrder;
    private ModelFieldInfo[] modelFieldInfos;
    private MonitorFieldInfo[] monitorFieldInfos;
    private ModelActionInfo[] modelActionInfos;
    private GroupInfo[] groupInfos;

    public MonitorFieldInfo[] getMonitorFieldInfos() {
        return monitorFieldInfos;
    }

    public void setMonitorFieldInfos(MonitorFieldInfo[] monitorFieldInfos) {
        this.monitorFieldInfos = monitorFieldInfos;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String[] getNameI18n() {
        return nameI18n;
    }

    public void setNameI18n(String[] nameI18n) {
        this.nameI18n = nameI18n;
    }

    public String[] getInfoI18n() {
        return infoI18n;
    }

    public void setInfoI18n(String[] infoI18n) {
        this.infoI18n = infoI18n;
    }

    public String getEntryAction() {
        return entryAction;
    }

    public void setEntryAction(String entryAction) {
        this.entryAction = entryAction;
    }

    public boolean isShowToMenu() {
        return showToMenu;
    }

    public void setShowToMenu(boolean showToMenu) {
        this.showToMenu = showToMenu;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public int getMenuOrder() {
        return menuOrder;
    }

    public void setMenuOrder(int menuOrder) {
        this.menuOrder = menuOrder;
    }

    public GroupInfo[] getGroupInfos() {
        return groupInfos;
    }

    public void setGroupInfos(GroupInfo[] groupInfos) {
        this.groupInfos = groupInfos;
    }

    public ModelFieldInfo[] getModelFieldInfos() {
        return modelFieldInfos;
    }

    public void setModelFieldInfos(ModelFieldInfo[] modelFieldInfos) {
        this.modelFieldInfos = modelFieldInfos;
    }

    public ModelActionInfo[] getModelActionInfos() {
        return modelActionInfos;
    }

    public void setModelActionInfos(ModelActionInfo[] modelActionInfos) {
        this.modelActionInfos = modelActionInfos;
    }
}
