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
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import qingzhou.xml.Doc;

class DocImpl implements Doc {
    private static final XPath xPathInstance;
    private static TransformerFactory transformerFactory;

    static {
        XPathFactory factory = XPathFactory.newInstance();
        xPathInstance = factory.newXPath();

        try {
            transformerFactory = TransformerFactory.newInstance();
        } catch (Throwable e) {
            transformerFactory = TransformerFactory.newInstance(
                    "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl",
                    DocImpl.class.getClassLoader());
        }
    }

    private final org.w3c.dom.Document dom;

    public DocImpl(org.w3c.dom.Document dom) {
        this.dom = dom;
    }

    @Override
    public String getText(String xPath) throws XPathExpressionException {
        return findNode(xPath).getTextContent();
    }

    @Override
    public Properties getNode(String xPath) throws XPathExpressionException {
        return getPropertiesFromNode(findNode(xPath));
    }

    @Override
    public List<Properties> getNodes(String xPath) throws XPathExpressionException {
        NodeList nodeList = findNodeNodeList(xPath);
        List<Properties> list = new ArrayList<>();
        int length = nodeList.getLength();
        for (int i = 0; i < length; i++) {
            Node item = nodeList.item(i);
            Properties propertiesFromNode = getPropertiesFromNode(item);
            list.add(propertiesFromNode);
        }
        return list;
    }

    @Override
    public void updateNode(String xPath, Properties newProperties) throws XPathExpressionException {
        Node node = findNode(xPath);

        for (String key : newProperties.stringPropertyNames()) {
            String value = newProperties.getProperty(key);
            ((org.w3c.dom.Element) node).setAttribute(key, value);
        }
    }

    @Override
    public void addNode(String parentXPath, String nodeName, Properties attributes) throws XPathExpressionException {
        Node parent = findNode(parentXPath);

        org.w3c.dom.Element newElement = dom.createElement(nodeName);
        for (String key : attributes.stringPropertyNames()) {
            String value = attributes.getProperty(key);
            newElement.setAttribute(key, value);
        }
        parent.appendChild(newElement);
    }

    @Override
    public void deleteNode(String xPath) throws XPathExpressionException {
        Node node = findNode(xPath);
        node.getParentNode().removeChild(node);
    }

    @Override
    public void write(File file) throws Exception {
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            StreamResult result = new StreamResult(outputStream);
            transformer.transform(new DOMSource(dom), result);
        }
    }

    private Node findNode(String xPath) throws XPathExpressionException {
        if (xPath == null || xPath.trim().isEmpty())
            throw new XPathExpressionException("xPath cannot be null or empty");

        Node obj = (Node) xPathInstance.evaluate(xPath, dom, XPathConstants.NODE);
        if (obj == null) throw new XPathExpressionException("xPath [" + xPath + "] is invalid");

        return obj;
    }

    private NodeList findNodeNodeList(String xPath) throws XPathExpressionException {
        if (xPath == null || xPath.trim().isEmpty())
            throw new XPathExpressionException("xPath cannot be null or empty");

        NodeList obj = (NodeList) xPathInstance.evaluate(xPath, dom, XPathConstants.NODESET);
        if (obj == null) throw new XPathExpressionException("xPath [" + xPath + "] is invalid");

        return obj;
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
