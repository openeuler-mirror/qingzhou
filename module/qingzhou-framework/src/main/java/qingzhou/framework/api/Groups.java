package qingzhou.framework.api;

import java.util.Arrays;
import java.util.List;

public interface Groups {
    List<Group> groups();

    static Groups of(Group... group) {
        return () -> Arrays.asList(group);
    }

    static Groups merge(Groups groups, Group... group) {
        if (groups == null) {
            groups = Groups.of();
        }
        groups.groups().addAll(Arrays.asList(group));
        return groups;
    }
}
