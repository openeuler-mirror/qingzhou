package qingzhou.api;

import java.util.Arrays;
import java.util.List;

public interface Groups {
    List<Group> groups();

    static Groups of(String... group) {
        return of(Arrays.stream(group).map(Group::of).toArray(Group[]::new));
    }

    static Groups of(Group... group) {
        return () -> Arrays.asList(group);
    }
}
