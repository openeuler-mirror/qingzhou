package qingzhou.api.console.group;

import java.util.Arrays;
import java.util.List;

public interface GroupManager {
    List<Group> groups();

    static GroupManager of(Group... group) {
        return () -> Arrays.asList(group);
    }
}
