package qingzhou.config;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import qingzhou.bootstrap.Utils;
import qingzhou.framework.util.FileUtil;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class XmlUtil {
    private static final XPath xpath;

    static {
        XPathFactory factory = XPathFactory.newInstance();
        xpath = factory.newXPath();
    }

    private final Document doc;
    private final File file;

    public XmlUtil(File path, boolean isNew) {
        this(path, null, isNew);
    }

    public XmlUtil(File f, InputStream xmlInputStream, boolean isNew) {
        this.file = f;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance("com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl",// 解决设置 -D参数冲突问题等
                    Thread.currentThread().getContextClassLoader());
            DocumentBuilder db = dbf.newDocumentBuilder();
            if (isNew) {
                this.doc = db.newDocument();
            } else {
                if (xmlInputStream == null && file != null) {
                    xmlInputStream = Files.newInputStream(file.toPath());
                }
                try (InputStream inputStream = new BufferedInputStream(Objects.requireNonNull(xmlInputStream))) {
                    this.doc = db.parse(inputStream);
                }
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new IllegalStateException(e);
        }
    }

    public XmlUtil(File file) {
        this(file, false);
    }

    public List<Map<String, String>> getAttributesList(String nodeExpression) {
        return Utils.getAttributesList(doc, nodeExpression);
    }

    public boolean isNodeExists(String nodeExpression) {
        Node node = (Node) evaluate(nodeExpression, doc, XPathConstants.NODE);
        return node != null;
    }

    public Map<String, String> getAttributes(String nodeExpression) {
        return getAttributesWithContent(nodeExpression);
    }

    public Map<String, String> getAttributesWithContent(String nodeExpression) {
        Node node = (Node) evaluate(nodeExpression, doc, XPathConstants.NODE);
        return getPropertiesWithContent(node);
    }

    public boolean setAttributes(String nodeExpression, Map<String, String> properties) {
        return setAttributesWithContent(nodeExpression, properties, null);
    }

    public boolean setAttributesWithContent(String nodeExpression, Map<String, String> properties, String content) {
        Element element = (Element) evaluate(nodeExpression, doc, XPathConstants.NODE);
        if (element != null) {
            if (properties != null) {
                for (Map.Entry<String, String> kv : properties.entrySet()) {
                    element.setAttribute(kv.getKey(), kv.getValue());
                }
            }
            if (content != null) {
                element.setTextContent(content);
            }
            return true;
        } else {
            return false;
        }
    }

    public void deleteAttribute(String nodeExpression, String key) {
        Element element = (Element) evaluate(nodeExpression, doc, XPathConstants.NODE);
        if (element != null) {
            element.removeAttribute(key);
        }
    }

    public void delete(String nodeExpression) {
        Element element = (Element) evaluate(nodeExpression, doc, XPathConstants.NODE);
        deleteNode(element);
    }

    public void addNew(String parentNodeExpression, String tagName, Map<String, String> properties) {
        addNewWithContent(parentNodeExpression, tagName, properties, null);
    }

    /****
     * 新增节点包括新增属性值和内容
     * @param parentNodeExpression  属性父节点层级表达式
     * @param tagName  节点tag名称
     * @param properties  属性
     * @param context     内容
     */
    public void addNewWithContent(String parentNodeExpression, String tagName, Map<String, String> properties, String context) {
        Element newElement = doc.createElement(tagName);
        if (newElement == null) {
            return;
        }
        if (properties != null) {
            properties.forEach((k, v) -> {
                if (v != null) {
                    newElement.setAttribute(k, v);
                }
            });
        }
        if (context != null) {
            newElement.setTextContent(context);
        }

        //root
        if (parentNodeExpression == null || parentNodeExpression.isEmpty() || parentNodeExpression.equals("/")) {
            appendChild(doc, newElement);
        } else {
            Element parentNode = (Element) evaluate(parentNodeExpression, doc, XPathConstants.NODE);
            appendChild(parentNode, newElement);
        }
    }

    /**
     * 系列操作最后调用这个方法写入到文件
     */
    public void write() {
        try {
            removeTextNode(doc);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(doc);

            // for #ITAIT-4496: 先写入临时文件后重命名文件
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(100 * 1024)) {
                StreamResult result = new StreamResult(outputStream);
                transformer.transform(source, result);

                String content = outputStream.toString("UTF-8")
                        .replaceAll("(?<=-->)(?=\\S)", System.lineSeparator()); // xml文件中的注释格式丢失，多行注释显示为一行

                // 最后修改主文件
                FileUtil.writeFile(file, content); // 真实文件写入明文
            }
        } catch (IOException | IllegalArgumentException | TransformerException |
                 TransformerFactoryConfigurationError t) {
            throw new IllegalStateException(t);
        }
    }

    /**
     * 在高于jdk8的版本中，序列化后会多出一次换行，删除 TEXT_NODE 类型的节点可避免此问题
     * 未找到控制方式，暂时通过此方法解决
     */
    private void removeTextNode(Node node) {
        NodeList childNodes = node.getChildNodes();
        int length = childNodes.getLength();
        for (int i = length - 1; i >= 0; i--) {
            Node item = childNodes.item(i);
            if (item.getNodeType() == Node.TEXT_NODE && item.getTextContent().trim().length() == 0) {
                node.removeChild(item);
            } else {
                removeTextNode(item);
            }
        }
    }

    private Map<String, String> getPropertiesWithContent(Node node) {
        return Utils.getAttributes(node);
    }

    /**
     * 添加新节点并控制缩进
     * Xml解析时会将换行与缩进解析为 TEXT_NODE 类型的节点，在该节点会影响后面新增节点的缩进
     */
    private void appendChild(Node parentNode, Node newChild) {
        if (parentNode == null || newChild == null) {
            return;
        }
        Node lastChild = parentNode.getLastChild();
        if (lastChild != null && lastChild.getNodeType() == Node.TEXT_NODE) {
            lastChild.setTextContent(lastChild.getTextContent().trim());
        }
        parentNode.appendChild(newChild);
    }

    private void deleteNode(Node node) {
        if (node == null) {
            return;
        }

        // 删除node节点后，节点前的TEXT_NODE节点会造成空白行，清理掉
        Node preNode = node.getPreviousSibling();
        if (preNode != null && preNode.getNodeType() == Node.TEXT_NODE) {
            preNode.setTextContent(preNode.getTextContent().trim());
        }
        Node parentNode = node.getParentNode();
        parentNode.removeChild(node);
    }

    private Object evaluate(String expression, Object item, QName returnType) {
        if (expression.contains(" or ") || expression.contains(" not ") || expression.contains("\"")) { // 预防 Xpath 注入漏洞
            throw new IllegalArgumentException(expression);
        }

        try {
            return XmlUtil.xpath.evaluate(expression, item, returnType);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }
}
