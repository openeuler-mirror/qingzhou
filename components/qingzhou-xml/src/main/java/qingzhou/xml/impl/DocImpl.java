package qingzhou.xml.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
    private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    // XPathFactory 线程安全且创建昂贵，复用一个实例；XPath 仍每次新建（非线程安全）
    private static final XPathFactory xPathFactory = XPathFactory.newInstance();

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
        if (!(node instanceof org.w3c.dom.Element)) {
            throw new XPathExpressionException("xPath [" + xPath + "] does not match an element node");
        }

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
        Node parent = node.getParentNode();
        if (parent == null) {
            throw new XPathExpressionException("xPath [" + xPath + "] has no parent node to remove from");
        }
        parent.removeChild(node);
    }

    @Override
    public void write(File file) throws Exception {
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        // 临时文件 + 原子替换：直接覆盖在崩溃/磁盘满时会留下半截 XML 配置
        Path target = file.toPath();
        Path temp = Files.createTempFile(target.getParent(), file.getName(), ".tmp");
        try {
            try (FileOutputStream outputStream = new FileOutputStream(temp.toFile())) {
                transformer.transform(new DOMSource(dom), new StreamResult(outputStream));
            }
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private Node findNode(String xPath) throws XPathExpressionException {
        if (xPath == null || xPath.trim().isEmpty())
            throw new XPathExpressionException("xPath cannot be null or empty");

        Node obj = (Node) newXPath().evaluate(xPath, dom, XPathConstants.NODE);
        if (obj == null) throw new XPathExpressionException("xPath [" + xPath + "] is invalid");

        return obj;
    }

    private NodeList findNodeNodeList(String xPath) throws XPathExpressionException {
        if (xPath == null || xPath.trim().isEmpty())
            throw new XPathExpressionException("xPath cannot be null or empty");

        NodeList obj = (NodeList) newXPath().evaluate(xPath, dom, XPathConstants.NODESET);
        if (obj == null) throw new XPathExpressionException("xPath [" + xPath + "] is invalid");

        return obj;
    }

    /**
     * XPath 非线程安全，每次求值新建实例。
     */
    private static XPath newXPath() {
        return xPathFactory.newXPath();
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
