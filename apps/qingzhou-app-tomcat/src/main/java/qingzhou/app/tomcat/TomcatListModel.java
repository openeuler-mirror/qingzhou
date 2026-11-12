package qingzhou.app.tomcat;

import java.util.List;
import java.util.Map;

import qingzhou.api.action.Page;

/**
 * Tomcat 列表型模型基类：模板方法固化「读 JMX → 过滤 → 分页」，
 * 子类仅需实现 {@link #loadData()}（数据来源）与 {@link #idKey()}（主键字段）。
 */
public abstract class TomcatListModel extends TomcatModelBase implements Page {

    protected abstract List<Map<String, String>> loadData() throws Exception;

    protected abstract String idKey();

    /** 读 JMX，JMX 读取本身会抛受检异常，交由调用方（loadData/monitor）统一上抛。 */
    protected <T> T readJmx(TomcatJmx.Reader<T> reader) throws Exception {
        return TomcatJmx.read(getTomcatBaseDir(), reader);
    }

    @Override
    public List<String[]> page(int pageNum, int pageSize, Map<String, String> query, String[] listFields) throws Exception {
        return buildListResult(filterByQuery(loadData(), query), pageNum, pageSize, listFields);
    }

    @Override
    public int total(Map<String, String> query) {
        return filterByQuery(loadDataUnchecked(), query).size();
    }

    @Override
    public boolean contains(String id) {
        return loadDataUnchecked().stream().anyMatch(m -> id.equals(m.get(idKey())));
    }

    /** total/contains 的接口契约不允许受检异常，此处转非受检异常上抛。 */
    private List<Map<String, String>> loadDataUnchecked() {
        try {
            return loadData();
        } catch (Exception e) {
            throw new IllegalStateException("failed to load Tomcat data", e);
        }
    }

    /** 按主键查找，供子类 show 复用。 */
    protected Map<String, String> findById(String id) throws Exception {
        return loadData().stream().filter(m -> id.equals(m.get(idKey()))).findFirst().orElse(null);
    }
}
