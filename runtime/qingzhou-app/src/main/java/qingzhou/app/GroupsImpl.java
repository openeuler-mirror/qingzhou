package qingzhou.app;

import qingzhou.api.Group;
import qingzhou.api.Groups;

import java.io.Serializable;
import java.util.List;

public class GroupsImpl implements Groups, Serializable {
    public final List<Group> groupList;

    public GroupsImpl(List<Group> groupList) {
        this.groupList = groupList;
    }

    @Override
    public List<Group> groups() {
        return groupList;
    }
}
