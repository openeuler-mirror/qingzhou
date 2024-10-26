package qingzhou.api;

public enum MsgLevel {
    INFO("info"), WARN("warn"), ERROR("error");
    public final String flag;

    MsgLevel(String flag) {
        this.flag = flag;
    }
}
