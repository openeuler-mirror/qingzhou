package qingzhou.xml.impl;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.xml.Doc;

public class XmlImplTest {
    @Test
    public void nullFile_parse_throwIllegalArgumentException() {
        XmlImpl xml = new XmlImpl();
        try {
            xml.parse(null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void notFile_parse_throwIllegalArgumentException() {
        XmlImpl xml = new XmlImpl();
        try {
            File file = new File("A non-existent file");
            xml.parse(file);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void normalXmlFile_parse_success() {
        XmlImpl xml = new XmlImpl();
        try {
            xml.init();
            File file = new File("pom.xml");
            Doc doc = xml.parse(file);
            Assert.assertNotNull(doc);
        } catch (Throwable e) {
            Assert.fail();
        }
    }
}
