package qingzhou.xml.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import qingzhou.xml.Doc;

class DocImpl implements Doc {
    private final org.w3c.dom.Document dom;
    private final XPath xPath;

    public DocImpl(org.w3c.dom.Document dom, XPath xPath) {
        this.dom = dom;
        this.xPath = xPath;
    }

    @Override
    public String getTextContent(String xPath) throws XPathExpressionException {
        if (xPath == null) return null;
        if (xPath.trim().isEmpty()) throw new XPathExpressionException("xPath cannot be empty");

        Node node = (Node) this.xPath.evaluate(xPath.trim(), dom, XPathConstants.NODE);
        if (node == null) return null;
        return node.getTextContent();
    }

    @Override
    public Properties getAttributes(String xPath) throws XPathExpressionException {
        if (xPath == null) return null;
        if (xPath.trim().isEmpty()) throw new XPathExpressionException("xPath cannot be empty");

        Node node = (Node) this.xPath.evaluate(xPath.trim(), dom, XPathConstants.NODE);
        if (node == null) return null;
        return getPropertiesFromNode(node);
    }

    @Override
    public List<Properties> getNodes(String xPath) throws XPathExpressionException {
        if (xPath == null) return null;
        if (xPath.trim().isEmpty()) throw new XPathExpressionException("xPath cannot be empty");

        NodeList nodeList = (NodeList) this.xPath.evaluate(xPath, dom, XPathConstants.NODESET);
        if (nodeList == null) return null;

        List<Properties> list = new ArrayList<>();
        int length = nodeList.getLength();
        for (int i = 0; i < length; i++) {
            Node item = nodeList.item(i);
            Properties propertiesFromNode = getPropertiesFromNode(item);
            list.add(propertiesFromNode);
        }
        return list;
    }

    private Properties getPropertiesFromNode(Node node) {
        NamedNodeMap namedNodeMap = node.getAttributes();
        int length = namedNodeMap.getLength();
        Properties result = new Properties();
        for (int i = 0; i < length; i++) {
            Node item = namedNodeMap.item(i);
            String nodeValue = item.getNodeValue();
            if (nodeValue != null) {
                result.put(item.getNodeName(), nodeValue);
            }
        }
        return result;
    }
}
