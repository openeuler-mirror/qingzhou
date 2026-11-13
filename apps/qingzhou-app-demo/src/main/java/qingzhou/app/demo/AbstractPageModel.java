package qingzhou.app.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import qingzhou.api.ModelBase;
import qingzhou.api.action.Page;

/**
 * 分页列表模型基类：内置「分页查询 + 关键字搜索 + 存在性判断」的通用实现。
 *
 * <h2>框架契约</h2>
 * 模型实现 {@link Page} 等「能力接口」后，{@code qingzhou.app.driver.DefaultAction} 会自动注入同名默认 Action，
 * 这些默认 Action 最终回调本类实现的 {@code page/total/contains} 方法（详见 DefaultAction 源码）。
 * 因此子类只需提供内存数据表 {@link #data()} 与 {@code @ModelField} 字段定义，无需重复编写列表逻辑。
 *
 * <h2>子类职责</h2>
 * <ol>
 *   <li>声明自己的内存表（建议 {@code static}，便于跨模型聚合统计，参见 {@code Dashboard}）；</li>
 *   <li>实现 {@link #data()} 返回该表；</li>
 *   <li>在构造器中写入示例数据。</li>
 * </ol>
 */
public abstract class AbstractPageModel extends ModelBase implements Page {

    /** 返回本模型的内存数据表，键为主键、值为整行记录。 */
    protected abstract Map<String, Map<String, String>> data();

    /** 关键字搜索：对任一查询字段做「包含」匹配（忽略大小写），全部满足才保留。 */
    private static boolean matches(Map<String, String> row, Map<String, String> query) {
        if (query == null || query.isEmpty()) return true;
        for (Map.Entry<String, String> e : query.entrySet()) {
            String v = e.getValue();
            if (v == null || v.isEmpty()) continue;
            String cell = row.get(e.getKey());
            if (cell == null || !cell.toLowerCase().contains(v.toLowerCase())) return false;
        }
        return true;
    }

    @Override
    public List<String[]> page(int pageNum, int pageSize, Map<String, String> query, String[] listFields) {
        List<Map<String, String>> rows = new ArrayList<>();
        for (Map<String, String> r : data().values()) {
            if (matches(r, query)) rows.add(r);
        }

        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, rows.size());

        List<String[]> result = new ArrayList<>();
        for (int i = from; i < to; i++) {
            Map<String, String> r = rows.get(i);
            String[] row = new String[listFields.length];
            for (int j = 0; j < listFields.length; j++) {
                String v = r.get(listFields[j]);
                row[j] = v != null ? v : "";
            }
            result.add(row);
        }
        return result;
    }

    @Override
    public int total(Map<String, String> query) {
        int n = 0;
        for (Map<String, String> r : data().values()) {
            if (matches(r, query)) n++;
        }
        return n;
    }

    @Override
    public boolean contains(String id) {
        return data().containsKey(id);
    }
}
