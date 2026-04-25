package qingzhou.app.driver.validation;

import qingzhou.dto.RequestImpl;
import qingzhou.dto.meta.annotation.ModelField;

public class ValidationContext {
    public final ModelField field;
    public final String parameter;
    public final RequestImpl request;

    public ValidationContext(ModelField field, String parameter, RequestImpl request) {
        this.field = field;
        this.parameter = parameter;
        this.request = request;
    }
}
