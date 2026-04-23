package qingzhou.xml.impl;

import java.io.File;
import java.util.Properties;
import javax.xml.xpath.XPathExpressionException;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.xml.Doc;

public class DocImplTest {
    @Test
    public void nullPath_getTextContent_returnNull() {
        Doc doc = getPomXmlDoc();
        try {
            String textContent = doc.getTextContent(null);
            Assert.assertNull(textContent);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void emptyPath_getTextContent_throwXPathExpressionException() {
        Doc doc = getPomXmlDoc();
        try {
            doc.getTextContent("");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof XPathExpressionException);
        }
    }

    @Test
    public void packagingPath_getTextContent_returnBundle() throws Exception {
        Doc doc = getPomXmlDoc();
        String packaging = doc.getTextContent("/project/packaging");
        Assert.assertEquals(packaging, "bundle");
    }

    @Test
    public void nullPath_getAttributes_returnNull() {
        Doc doc = getPomXmlDoc();
        try {
            Properties properties = doc.getAttributes(null);
            Assert.assertNull(properties);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void emptyPath_getAttributes_throwXPathExpressionException() {
        Doc doc = getPomXmlDoc();
        try {
            doc.getAttributes("");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof XPathExpressionException);
        }
    }

    @Test
    public void pomXmlProjectPath_getAttributes_returnProjectAttributes() {
        Doc doc = getPomXmlDoc();
        try {
            Properties properties = doc.getAttributes("/project");
            Assert.assertNotNull(properties);
            Assert.assertFalse(properties.isEmpty());
            String xmlns = properties.getProperty("xmlns");
            Assert.assertEquals(xmlns, "http://maven.apache.org/POM/4.0.0");
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void nullPath_getNodes_returnNull() {
        Doc doc = getPomXmlDoc();
        try {
            Object object = doc.getNodes(null);
            Assert.assertNull(object);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void emptyPath_getNodes_throwXPathExpressionException() {
        Doc doc = getPomXmlDoc();
        try {
            doc.getNodes("");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof XPathExpressionException);
        }
    }

    private Doc getPomXmlDoc() {
        try {
            XmlImpl xml = new XmlImpl();
            xml.init();
            return xml.parse(new File("pom.xml"));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            throw new IllegalStateException(e);
        }
    }
}
