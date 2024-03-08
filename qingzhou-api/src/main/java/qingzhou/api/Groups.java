package qingzhou.api;

import java.util.Arrays;
import java.util.List;

/**
 * 页面表单字段的分组。
 */
public interface Groups {

    /**
     * 获取页面表单字段分组的列表。
     * @return 返回一个页面表单字段分组的列表。
     */
    List<Group> groups();

    /**
     * 通过字符串数组创建一个Groups实例。
     * @param group 包含页面表单字段分组名称的字符串数组。
     * @return 返回一个Groups实例，其中包含了传入的组名。
     */
    static Groups of(String... group) {
        // 将字符串数组转换为Group数组，然后创建Groups实例
        return of(Arrays.stream(group).map(Group::of).toArray(Group[]::new));
    }

    /**
     * 通过Group数组创建一个Groups实例。
     * @param group Group对象数组。
     * @return 返回一个Groups实例，其中包含了传入的Group对象。
     */
    static Groups of(Group... group) {
        // 直接返回一个包含传入Group对象数组的Lists
        return () -> Arrays.asList(group);
    }
}

