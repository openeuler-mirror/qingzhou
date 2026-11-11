package qingzhou.dto.meta;

import java.util.Objects;

public abstract class Base {
    // 以下默认值须与注解默认值一致：annotation.json 仅记录非默认值，缺省字段依赖此初值
    public String code = "";

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
