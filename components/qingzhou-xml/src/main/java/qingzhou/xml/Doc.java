package qingzhou.xml;

import java.io.File;
import java.util.List;
import java.util.Properties;
import javax.xml.xpath.XPathExpressionException;

public interface Doc {
    String getText(String xPath) throws XPathExpressionException;

    Properties getNode(String xPath) throws XPathExpressionException;

    List<Properties> getNodes(String xPath) throws XPathExpressionException;

    void updateNode(String xPath, Properties attributes) throws XPathExpressionException;

    void addNode(String parentXPath, String nodeName, Properties attributes) throws XPathExpressionException;

    void deleteNode(String xPath) throws XPathExpressionException;

    void write(File file) throws Exception;
}
