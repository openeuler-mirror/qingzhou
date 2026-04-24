package qingzhou.xml;

import java.io.File;
import java.util.List;
import java.util.Properties;
import javax.xml.xpath.XPathExpressionException;

public interface Doc {
    String getTextContent(String xPath) throws XPathExpressionException;

    Properties getAttributes(String xPath) throws XPathExpressionException;

    List<Properties> getNodes(String xPath) throws XPathExpressionException;

    void updateNode(String xPath, Properties attributes) throws Exception;

    void addNode(String parentXPath, String elementName, Properties attributes) throws Exception;

    void deleteNode(String xPath) throws Exception;

    void write(File file) throws Exception;
}
