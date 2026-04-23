package qingzhou.xml;

import java.util.List;
import java.util.Properties;
import javax.xml.xpath.XPathExpressionException;

public interface Doc {
    String getTextContent(String xPath) throws XPathExpressionException;

    Properties getAttributes(String xPath) throws XPathExpressionException;

    List<Properties> getNodes(String xPath) throws XPathExpressionException;
}
