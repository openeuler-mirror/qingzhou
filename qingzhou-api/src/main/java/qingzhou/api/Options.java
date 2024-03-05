package qingzhou.api;

import java.util.Arrays;
import java.util.List;

/**
 * Options接口定义了配置选项的集合。
 */
public interface Options {

    /**
     * 获取配置选项的列表。
     * @return 一个包含所有配置选项的List集合。
     */
    List<Option> options();

    /**
     * 基于字符串数组创建Options实例。
     * @param option 字符串数组，每个元素将被转换为一个Option实例。
     * @return 返回一个包含给定选项的Options实例。
     */
    static Options of(String... option) {
        // 将字符串数组转换为Option数组，然后创建Options实例
        return of(Arrays.stream(option).map(Option::of).toArray(Option[]::new));
    }

    /**
     * 基于Option对象数组创建Options实例。
     * @param option Option对象数组，代表不同的配置选项。
     * @return 返回一个包含给定Option对象的Options实例。
     */
    static Options of(Option... option) {
        // 直接使用给定的Option数组创建Options实例
        return () -> Arrays.asList(option);
    }
}

