package qingzhou.dto.meta.annotation;

public abstract class Base {
    public String code;

    @Override
    public final boolean equals(Object object) {
        if (!(object instanceof Base)) return false;

        Base that = (Base) object;
        return code.equals(that.code);
    }

    @Override
    public final int hashCode() {
        return code.hashCode();
    }
}