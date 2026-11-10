package qingzhou.llm.impl;

import java.util.function.Supplier;

// 惰性值：成功只算一次、失败定局不重试、重入报错；失败不重试是因 provider 可能带去重表等副作用。
// null 结果无需额外标志，求值后 provider 随初始委托释放；置于未导出 impl 包，避免实现细节随 API 外泄。
// delegate 用 volatile 发布以保证快路径无锁可见；computing/computed 仅在同步块内读写，构成并发双检。
public final class Lazy<T> implements Supplier<T> {
    private volatile Supplier<T> delegate;
    private boolean computing;
    private boolean computed;

    public Lazy(Supplier<T> provider) {
        delegate = () -> {
            synchronized (this) {
                if (computing) throw new IllegalStateException("recursive initialization of Lazy");
                if (computed) return delegate.get();
                computing = true;
                try {
                    T value = provider.get();
                    delegate = () -> value;
                    return value;
                } catch (RuntimeException | Error e) {
                    delegate = () -> {
                        throw e;
                    };
                    throw e;
                } finally {
                    computing = false;
                    computed = true;
                }
            }
        };
    }

    @Override
    public T get() {
        return delegate.get();
    }
}
