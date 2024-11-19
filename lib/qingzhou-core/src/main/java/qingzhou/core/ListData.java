package qingzhou.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ListData implements Serializable {
    public static final String PAGE_NUM = "pageNum";

    public List<String[]> dataList = new ArrayList<>();
    public int totalSize;
    public int pageSize;
    public int pageNum;
}
