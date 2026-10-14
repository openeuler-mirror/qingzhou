package qingzhou.dto.meta.annotation;

import java.util.Objects;

public abstract class Base {
    public String code;

    @Override
    public final boolean equals(Object object) {
        if (!(object instanceof Base)) return false;

        Base that = (Base) object;
        return Objects.equals(code, that.code); // code 为 null 的元数据进入 HashSet 不得抛 NPE
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(code);
    }
}
