package qingzhou.framework.api;

import java.util.Arrays;
import java.util.List;

public interface Options {
    List<Option> options();

    static Options of(Option... option) {
        return () -> Arrays.asList(option);
    }

    static Options merge(Options options, Options... otherOptions) {
        if (options == null) {
            options = Options.of();
        }
        if (otherOptions != null) {
            for (Options otherOption : otherOptions) {
                Options finalOptions = options;
                otherOption.options().forEach(option -> finalOptions.options().add(option));
            }
        }
        return options;
    }

    static Options merge(Options options, Option... option) {
        if (options == null) {
            options = Options.of();
        }
        if (option != null) {
            for (Option opt : option) {
                if (opt != null) {
                    options.options().add(opt);
                }
            }
        }
        return options;
    }
}
