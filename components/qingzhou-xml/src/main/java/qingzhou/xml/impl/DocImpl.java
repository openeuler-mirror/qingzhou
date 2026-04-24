package qingzhou.xml.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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

    @Override
    public void updateNode(String xPath, Properties attributes) throws Exception {
        Node node = (Node) this.xPath.evaluate(xPath, dom, XPathConstants.NODE);
        if (node == null) {
            throw new IllegalArgumentException("Node not found: " + xPath);
        }

        for (String key : attributes.stringPropertyNames()) {
            String value = attributes.getProperty(key);
            if (value != null && !value.isEmpty()) {
                ((org.w3c.dom.Element) node).setAttribute(key, value);
            } else {
                ((org.w3c.dom.Element) node).removeAttribute(key);
            }
        }
    }

    @Override
    public void addNode(String parentXPath, String elementName, Properties attributes) throws Exception {
        Node parent = (Node) this.xPath.evaluate(parentXPath, dom, XPathConstants.NODE);
        if (parent == null) {
            throw new IllegalArgumentException("Parent node not found: " + parentXPath);
        }

        org.w3c.dom.Element newElement = dom.createElement(elementName);
        for (String key : attributes.stringPropertyNames()) {
            String value = attributes.getProperty(key);
            if (value != null && !value.isEmpty()) {
                newElement.setAttribute(key, value);
            }
        }
        parent.appendChild(newElement);
    }

    @Override
    public void deleteNode(String xPath) throws Exception {
        Node node = (Node) this.xPath.evaluate(xPath, dom, XPathConstants.NODE);
        if (node == null) {
            throw new IllegalArgumentException("Node not found: " + xPath);
        }
        node.getParentNode().removeChild(node);
    }

    @Override
    public void write(File file) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount}", "4");
        DOMSource source = new DOMSource(dom);
        FileOutputStream fos = new FileOutputStream(file);
        StreamResult result = new StreamResult(fos);
        transformer.transform(source, result);
        fos.close();
    }
}
