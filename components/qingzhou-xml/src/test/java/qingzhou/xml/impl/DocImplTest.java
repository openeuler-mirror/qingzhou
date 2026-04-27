package qingzhou.xml.impl;

import java.io.File;
import java.util.Properties;
import javax.xml.xpath.XPathExpressionException;

import org.testng.Assert;
import org.testng.annotations.Test;
import qingzhou.xml.Doc;

public class DocImplTest {
    @Test
    public void nullPath_getText_throwXPathExpressionException() {
        Doc doc = getPomXmlDoc();
        try {
            doc.getText(null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof XPathExpressionException);
        }
    }

    @Test
    public void emptyPath_getText_throwXPathExpressionException() {
        Doc doc = getPomXmlDoc();
        try {
            doc.getText("");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof XPathExpressionException);
        }
    }

    @Test
    public void packagingPath_getText_returnBundle() throws Exception {
        Doc doc = getPomXmlDoc();
        String packaging = doc.getText("/project/packaging");
        Assert.assertEquals(packaging, "bundle");
    }

    @Test
    public void nullPath_getNode_throwXPathExpressionException() {
        Doc doc = getPomXmlDoc();
        try {
            doc.getNode(null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof XPathExpressionException);
        }
    }

    @Test
    public void emptyPath_getNode_throwXPathExpressionException() {
        Doc doc = getPomXmlDoc();
        try {
            doc.getNode("");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof XPathExpressionException);
        }
    }

    @Test
    public void pomXmlProjectPath_getAttributes_returnProjectNodeAttributes() {
        Doc doc = getPomXmlDoc();
        try {
            Properties properties = doc.getNode("/project");
            Assert.assertNotNull(properties);
            Assert.assertFalse(properties.isEmpty());
            String xmlns = properties.getProperty("xmlns");
            Assert.assertEquals(xmlns, "http://maven.apache.org/POM/4.0.0");
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void nullPath_getNodes_throwXPathExpressionException() {
        Doc doc = getPomXmlDoc();
        try {
            doc.getNodes(null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof XPathExpressionException);
        }
    }

    @Test
    public void emptyPath_getNodes_Attributes_throwXPathExpressionException() {
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
