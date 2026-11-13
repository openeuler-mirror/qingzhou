package qingzhou.app.demo;

import java.util.LinkedHashMap;
import java.util.Map;

import qingzhou.api.action.Add;
import qingzhou.api.action.Delete;
import qingzhou.api.action.Show;
import qingzhou.api.action.Update;

/**
 * 内存型 CRUD 模型基类：在 {@link AbstractPageModel} 的分页能力之上，
 * 统一提供「详情 / 新增 / 更新 / 删除」的通用实现。
 *
 * <h2>框架契约</h2>
 * 实现 {@link Show}/{@link Add}/{@link Update}/{@link Delete} 后，框架自动注入默认 Action，
 * 最终回调本类的 {@code show/add/update/delete}。子类仅需：
 * <ol>
 *   <li>实现 {@link #genId(Map)} 生成主键并回填（如 id、创建时间）；</li>
 *   <li>如需文件上传等额外处理，覆写钩子 {@link #onAdd(String, Map)} / {@link #onUpdate(String, Map)}。</li>
 * </ol>
 *
 * <h2>设计约定</h2>
 * 内存表为各子类自己的静态字段（见 {@link #data()}），保证不同模型的数据相互隔离；
 * 主键计数器为实例字段，轻舟应用模型为单例，足以保证新增主键唯一。
 */
public abstract class AbstractCrudModel extends AbstractPageModel implements Show, Add, Update, Delete {

    /** 主键自增序号（格式化 3 位），供 {@link #genId(Map)} 拼装主键前缀，如 S001。 */
    protected int idCounter = 1;

    /** 生成主键：回填 {@code data}（如 id、创建时间）并返回该主键，作为内存表键。 */
    protected abstract String genId(Map<String, String> data);

    /** 新增前钩子（默认空），用于文件上传等额外处理。 */
    protected void onAdd(String id, Map<String, String> data) throws Exception {
    }

    /** 更新前钩子（默认空）。 */
    protected void onUpdate(String id, Map<String, String> data) throws Exception {
    }

    @Override
    public Map<String, String> show(String id) {
        return data().get(id);
    }

    @Override
    public void add(Map<String, String> data) throws Exception {
        String id = genId(data);
        onAdd(id, data);
        data().put(id, new LinkedHashMap<>(data));
    }

    @Override
    public void update(String id, Map<String, String> data) throws Exception {
        Map<String, String> existing = data().get(id);
        if (existing != null) {
            onUpdate(id, data);
            existing.putAll(data);
        }
    }

    @Override
    public void delete(String id) {
        data().remove(id);
    }

    /** 取下一个自增序号（3 位补零）。 */
    protected String nextSeq() {
        return String.format("%03d", idCounter++);
    }
}
