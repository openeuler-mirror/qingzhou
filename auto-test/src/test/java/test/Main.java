package test;

import org.testng.Assert;
import org.testng.annotations.Test;

public class Main {
    @Test
    public void test() {
        System.out.println("运行测试。。。");
        Assert.assertTrue(true);
    }
}
