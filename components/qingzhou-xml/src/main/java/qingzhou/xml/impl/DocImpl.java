package qingzhou.xml.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.xml.XMLConstants;
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
    private static final TransformerFactory transformerFactory;

    static {
        TransformerFactory tf = TransformerFactory.newInstance();
        try {
            tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            // 与 XmlImpl 一致，禁止 DTD，杜绝 XXE
            tf.setAttribute("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception ignored) {
        }
        transformerFactory = tf;
    }

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
        Path parent = target.getParent() != null ? target.getParent() : Paths.get("."); // 相对路径无父目录
        Path temp = Files.createTempFile(parent, file.getName(), ".tmp");
        try {
            try { // XML 配置可能含敏感信息，落盘即收敛权限（非 POSIX 平台跳过）
                Files.setPosixFilePermissions(temp, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException | IOException ignored) {
            }
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
        return evaluate(xPath, XPathConstants.NODE);
    }

    private NodeList findNodeNodeList(String xPath) throws XPathExpressionException {
        return evaluate(xPath, XPathConstants.NODESET);
    }

    // XPath 求值公共逻辑：空白校验 → 求值 → 判空报错
    @SuppressWarnings("unchecked")
    private <T> T evaluate(String xPath, javax.xml.namespace.QName returnType) throws XPathExpressionException {
        if (isBlankXPath(xPath))
            throw new XPathExpressionException("xPath cannot be null or empty");

        T result = (T) newXPath().evaluate(xPath, dom, returnType);
        if (result == null) throw new XPathExpressionException("xPath [" + xPath + "] is invalid");

        return result;
    }

    private static boolean isBlankXPath(String xPath) {
        return xPath == null || xPath.trim().isEmpty();
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
