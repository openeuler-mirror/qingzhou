package qingzhou.api;

import java.util.Arrays;
import java.util.List;

public interface Options {
    List<Option> options();

    static Options of(String... option) {
        return of(Arrays.stream(option).map(Option::of).toArray(Option[]::new));
    }

    static Options of(Option... option) {
        return () -> Arrays.asList(option);
    }
}
