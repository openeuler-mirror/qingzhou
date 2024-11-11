package qingzhou.deployer;

import java.util.ArrayList;
import java.util.List;

public class ListData extends ResponseData {
    public static final String PAGE_NUM = "pageNum";

    public List<String[]> dataList = new ArrayList<>();
    public int totalSize;
    public int pageSize;
    public int pageNum;
}
