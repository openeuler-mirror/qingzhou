package qingzhou.api.console.option;

import java.util.Arrays;
import java.util.List;

public interface OptionManager {
    List<Option> options();

    static OptionManager of(Option... options) {
        return () -> Arrays.asList(options);
    }

}
