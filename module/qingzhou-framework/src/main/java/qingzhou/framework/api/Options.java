package qingzhou.framework.api;

import java.util.Arrays;
import java.util.List;

public interface Options {
    List<Option> options();

    static Options of(Option... option) {
        return () -> Arrays.asList(option);
    }

    static Options merge(Options options, Option... option) {
        if (options == null) {
            options = Options.of();
        }
        options.options().addAll(Arrays.asList(option));
        return options;
    }
}
