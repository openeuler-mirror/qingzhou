package qingzhou.registry;

public class ActionPageInfo {
    public final String icon;
    public final String forwardTo;
    public final int shownOnList;
    public final int shownOnListHead;

    public ActionPageInfo(String icon, String forwardTo, int shownOnList, int shownOnListHead) {
        this.icon = icon;
        this.forwardTo = forwardTo;
        this.shownOnList = shownOnList;
        this.shownOnListHead = shownOnListHead;
    }
}
