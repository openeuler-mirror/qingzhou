package qingzhou.json.impl;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.json.Json;

import java.util.*;

public class JsonImplTest {

    // 每次测试前创建新实例，确保测试间独立无共享状态
    private static JsonImpl createJsonImpl() {
        JsonImpl impl = new JsonImpl();
        impl.init(); // OSGi @Activate 不会自动调用，需手动初始化 ObjectMapper
        return impl;
    }

    // ==================== toJson(Object) 序列化测试 ====================

    @Test
    public void dataNull_toJson_returnNullString() throws Exception {
        Json json = createJsonImpl();
        String result = json.toJson(null);
        Assert.assertEquals(result, "null");
    }

    @Test
    public void dataEmptyBean_toJson_returnEmptyObjectJson() throws Exception {
        Json json = createJsonImpl();
        String result = json.toJson(new EmptyBean());
        Assert.assertEquals(result, "{}");
    }

    @Test
    public void dataSimpleBean_toJson_returnCorrectJson() throws Exception {
        Json json = createJsonImpl();
        SimpleBean bean = new SimpleBean();
        bean.setName("test");
        bean.setAge(25);
        String result = json.toJson(bean);
        Assert.assertTrue(result.contains("\"name\":\"test\""));
        Assert.assertTrue(result.contains("\"age\":25"));
    }

    @Test
    public void dataNestedBean_toJson_returnNestedJson() throws Exception {
        Json json = createJsonImpl();
        OuterBean outer = new OuterBean();
        outer.setId(1);
        InnerBean inner = new InnerBean();
        inner.setValue("nested");
        outer.setInner(inner);
        String result = json.toJson(outer);
        Assert.assertTrue(result.contains("\"id\":1"));
        Assert.assertTrue(result.contains("\"inner\":{"));
        Assert.assertTrue(result.contains("\"value\":\"nested\""));
    }

    @Test
    public void dataArray_toJson_returnArrayJson() throws Exception {
        Json json = createJsonImpl();
        int[] array = {1, 2, 3};
        String result = json.toJson(array);
        Assert.assertTrue(result.contains("[1,2,3]"));
    }

    @Test
    public void dataList_toJson_returnListJson() throws Exception {
        Json json = createJsonImpl();
        List<String> list = Arrays.asList("a", "b", "c");
        String result = json.toJson(list);
        Assert.assertTrue(result.contains("[\"a\",\"b\",\"c\"]"));
    }

    @Test
    public void dataMap_toJson_returnMapJson() throws Exception {
        Json json = createJsonImpl();
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", 100);
        String result = json.toJson(map);
        Assert.assertTrue(result.contains("\"key1\":\"value1\""));
        Assert.assertTrue(result.contains("\"key2\":100"));
    }

    // ==================== fromJson(String, Class) 反序列化测试 ====================

    @Test
    public void dataNullString_fromJson_returnNull() throws Exception {
        Json json = createJsonImpl();
        SimpleBean result = json.fromJson("null", SimpleBean.class);
        Assert.assertNull(result);
    }

    @Test
    public void dataEmptyObjectJson_fromJson_returnEmptyBean() throws Exception {
        Json json = createJsonImpl();
        SimpleBean result = json.fromJson("{}", SimpleBean.class);
        Assert.assertNotNull(result);
        Assert.assertNull(result.getName());
        Assert.assertEquals(result.getAge(), 0);
    }

    @Test
    public void dataSimpleJson_fromJson_returnCorrectBean() throws Exception {
        Json json = createJsonImpl();
        SimpleBean result = json.fromJson("{\"name\":\"hello\",\"age\":30}", SimpleBean.class);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getName(), "hello");
        Assert.assertEquals(result.getAge(), 30);
    }

    @Test
    public void dataNestedJson_fromJson_returnNestedBean() throws Exception {
        Json json = createJsonImpl();
        OuterBean result = json.fromJson(
                "{\"id\":99,\"inner\":{\"value\":\"deep\"}}", OuterBean.class);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getId(), 99);
        Assert.assertNotNull(result.getInner());
        Assert.assertEquals(result.getInner().getValue(), "deep");
    }

    @Test
    public void dataArrayJson_fromJson_returnArray() throws Exception {
        Json json = createJsonImpl();
        String[] result = json.fromJson("[\"x\",\"y\",\"z\"]", String[].class);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.length, 3);
        Assert.assertEquals(result[0], "x");
        Assert.assertEquals(result[1], "y");
        Assert.assertEquals(result[2], "z");
    }

    // ==================== 往返测试：toJson + fromJson ====================

    @Test
    public void dataSimpleBean_toJsonThenFromJson_restoreOriginalData() throws Exception {
        Json json = createJsonImpl();
        SimpleBean original = new SimpleBean();
        original.setName("roundtrip");
        original.setAge(42);
        String jsonStr = json.toJson(original);
        SimpleBean restored = json.fromJson(jsonStr, SimpleBean.class);
        Assert.assertEquals(restored.getName(), original.getName());
        Assert.assertEquals(restored.getAge(), original.getAge());
    }

    @Test
    public void dataComplexBeanWithListMap_toJsonThenFromJson_restoreOriginalData() throws Exception {
        Json json = createJsonImpl();
        ComplexBean original = new ComplexBean();
        original.setTitle("complex");
        original.setTags(Arrays.asList("java", "json", "testng"));
        Map<String, String> attrs = new HashMap<>();
        attrs.put("author", "dev");
        attrs.put("version", "1.0");
        original.setAttributes(attrs);
        original.setActive(true);
        String jsonStr = json.toJson(original);
        ComplexBean restored = json.fromJson(jsonStr, ComplexBean.class);
        Assert.assertEquals(restored.getTitle(), original.getTitle());
        Assert.assertEquals(restored.getTags(), original.getTags());
        Assert.assertEquals(restored.getAttributes(), original.getAttributes());
        Assert.assertEquals(restored.isActive(), original.isActive());
    }

    @Test
    public void dataStringValue_toJsonThenFromJson_restoreOriginalData() throws Exception {
        Json json = createJsonImpl();
        String original = "hello world";
        String jsonStr = json.toJson(original);
        String restored = json.fromJson(jsonStr, String.class);
        Assert.assertEquals(restored, original);
    }

    @Test
    public void dataIntegerValue_toJsonThenFromJson_restoreOriginalData() throws Exception {
        Json json = createJsonImpl();
        Integer original = 12345;
        String jsonStr = json.toJson(original);
        Integer restored = json.fromJson(jsonStr, Integer.class);
        Assert.assertEquals(restored, original);
    }

    @Test
    public void dataBooleanValue_toJsonThenFromJson_restoreOriginalData() throws Exception {
        Json json = createJsonImpl();
        Boolean original = true;
        String jsonStr = json.toJson(original);
        Boolean restored = json.fromJson(jsonStr, Boolean.class);
        Assert.assertEquals(restored, original);
    }

    @Test
    public void dataListOfBeans_toJsonThenFromJson_restoreOriginalData() throws Exception {
        Json json = createJsonImpl();
        List<SimpleBean> original = new ArrayList<>();
        SimpleBean b1 = new SimpleBean();
        b1.setName("one");
        b1.setAge(1);
        SimpleBean b2 = new SimpleBean();
        b2.setName("two");
        b2.setAge(2);
        original.add(b1);
        original.add(b2);
        String jsonStr = json.toJson(original);
        @SuppressWarnings("unchecked")
        List<SimpleBean> restored = json.fromJson(jsonStr, List.class);
        Assert.assertEquals(restored.size(), 2);
    }

    @Test
    public void dataUnknownPropertiesJson_fromJson_returnBeanIgnoreUnknown() throws Exception {
        Json json = createJsonImpl();
        SimpleBean result = json.fromJson(
                "{\"name\":\"test\",\"age\":10,\"extraField\":\"shouldBeIgnored\"}",
                SimpleBean.class);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getName(), "test");
        Assert.assertEquals(result.getAge(), 10);
    }

    // ==================== 内部测试Bean类 ====================

    public static class EmptyBean {
    }

    public static class SimpleBcdean {
        private String name;
        private int age;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }

    public static class InnerBean {
        private String value;

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    public static class OuterBean {
        private int id;
        private InnerBean inner;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public InnerBean getInner() { return inner; }
        public void setInner(InnerBean inner) { this.inner = inner; }
    }

    public static class ComplexBean {
        private String title;
        private List<String> tags;
        private Map<String, String> attributes;
        private boolean active;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public Map<String, String> getAttributes() { return attributes; }
        public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}
