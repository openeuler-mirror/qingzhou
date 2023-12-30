package qingzhou.framework.util;

import org.w3c.dom.*;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

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

    public XmlUtil(InputStream xmlInputStream, boolean isNew) {
        this(null, xmlInputStream, isNew);
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
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public XmlUtil(File file) {
        this(file, false);
    }

    public static Properties getProperties(File file, String nodeExpression) {
        XmlUtil xmlUtil = new XmlUtil(file);
        Map<String, String> attributes = xmlUtil.getAttributes(nodeExpression);
        Properties properties = ObjectUtil.map2Properties(attributes);
        return properties.size() == 0 ? null : properties;
    }

    public boolean containsNode(String nodeName, String attrName, String attrValue) {
        return containsNode(String.format("//%s[@%s='%s']", nodeName, attrName, attrValue));
    }

    public boolean containsNode(String nodeExpression) {
        String xpathExpression = String.format("boolean(%s)", nodeExpression);
        try {
            return Boolean.parseBoolean(xpath.evaluate(xpathExpression, doc));
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getAttributeList(String nodeName, String attrName) {
        return getAttributeList(String.format("//%s/@%s", nodeName, attrName));
    }

    public List<String> getAttributeList(String nodeExpression) {
        NodeList nodeList;
        try {
            nodeList = (NodeList) xpath.evaluate(nodeExpression, doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        if (nodeList == null || nodeList.getLength() == 0) {
            return null;
        }

        List<String> attributeList = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            attributeList.add(nodeList.item(i).getNodeValue());
        }

        return attributeList;
    }

    public List<Map<String, String>> getAttributesList(String nodeExpression) {
        return getAttributesListWithContentByMatcher(nodeExpression, null, null);
    }

    public List<Map<String, String>> getAttributesListWithContentByMatcher(String nodeExpression, String textContentKey, PropertiesMatcher matcher) {
        NodeList nodeList = (NodeList) evaluate(nodeExpression, doc, XPathConstants.NODESET);
        if (nodeList == null || nodeList.getLength() == 0) {
            return null;
        }

        List<Map<String, String>> list = new ArrayList<>();
        int length = nodeList.getLength();
        for (int i = 0; i < length; i++) {
            Node item = nodeList.item(i);
            Map<String, String> properties = getPropertiesWithContent(item, textContentKey);
            if (matcher == null) {
                list.add(properties);
            } else {
                if (matcher.matches(properties)) {
                    list.add(properties);
                    if (matcher.matchOne()) {
                        return list;
                    }
                }
            }
        }

        return list;
    }

    public boolean isNodeExists(String nodeExpression) {
        Node node = (Node) evaluate(nodeExpression, doc, XPathConstants.NODE);
        return node != null;
    }

    public boolean getBooleanAttribute(String nodeExpression, String specifiedKey, boolean defaultBoolean) {
        String val = getSpecifiedAttribute(nodeExpression, specifiedKey);
        if (val != null) {
            return Boolean.parseBoolean(val);
        } else {
            return defaultBoolean;
        }
    }

    public String getSpecifiedAttribute(String nodeExpression, String specifiedKey) {
        // todo 有没有更高效的方式？
        Map<String, String> attributes = getSpecifiedAttributes(nodeExpression, new String[]{specifiedKey});
        if (attributes == null) return null;
        return attributes.get(specifiedKey);
    }

    public Map<String, String> getSpecifiedAttributes(String nodeExpression, String[] specifiedKeys) {
        // todo 有没有更高效的方式？
        Map<String, String> attributes = getAttributes(nodeExpression);
        if (attributes == null) return null;
        Map<String, String> properties = new LinkedHashMap<>();
        for (String s : specifiedKeys) {
            properties.put(s, attributes.get(s));
        }
        return properties;
    }

    public List<String> getSpecifiedListAttributeByAttr(String tagName, String specifiedKey, String attrKey, String attrValue) {
        String nodeExpression = "//" + tagName + "/@" + specifiedKey + "[@" + attrKey + "='" + attrValue + "']";
        return getAttributeList(nodeExpression);
    }

    public int getTotalSize(String tagName) {
        NodeList nodes = (NodeList) evaluate("//" + tagName, doc, XPathConstants.NODESET);
        return nodes.getLength();
    }

    public Map<String, String> getAttributesByKey(String nodeExpression, String keyName, String keyValue) {
        return getAttributes(String.format(nodeExpression + "[@%s='%s']", keyName, keyValue));
    }

    public Map<String, String> getAttributes(String nodeExpression) {
        return getAttributesWithContent(nodeExpression, null);
    }

    public Map<String, String> getAttributesWithContent(String nodeExpression, String textContentKey) {
        Node node = (Node) evaluate(nodeExpression, doc, XPathConstants.NODE);
        return getPropertiesWithContent(node, textContentKey);
    }

    public Map<String, String> getAttributesByMatcher(String nodeExpression, PropertiesMatcher matcher) {
        List<Map<String, String>> list = getAttributesListWithContentByMatcher(nodeExpression, null, new PropertiesMatcher() {
            @Override
            public boolean matchOne() {
                return true;
            }

            @Override
            public boolean matches(Map<String, String> check) {
                return matcher.matches(check);
            }
        });

        if (list == null || list.isEmpty()) return null;
        return list.get(0);
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

    public void setAttribute(String nodeExpression, String key, String value) {
        Element element = (Element) evaluate(nodeExpression, doc, XPathConstants.NODE);
        if (element != null) {
            element.setAttribute(key, value);
        }
    }

    public void setAttributeForAllNode(String nodeExpression, String key, String value) {
        NodeList nodeList = (NodeList) evaluate(nodeExpression, doc, XPathConstants.NODE);
        if (nodeList != null) {
            int length = nodeList.getLength();
            for (int i = 0; i < length; i++) {
                Element element = (Element) nodeList.item(i);
                element.setAttribute(key, value);
            }
        }
    }

    public void setAttributesByMatcher(String nodeExpression, PropertiesMatcher matcher, String key, String val) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put(key, val);
        setAttributesByMatcher(nodeExpression, matcher, p);
    }

    public void setAttributesByMatcher(String nodeExpression, PropertiesMatcher matcher, Map<String, String> data) {
        NodeList nodeList = (NodeList) evaluate(nodeExpression, doc, XPathConstants.NODESET);
        if (nodeList != null) {
            int length = nodeList.getLength();
            for (int i = 0; i < length; i++) {
                Element item = (Element) nodeList.item(i);
                Map<String, String> properties = getPropertiesWithContent(item, null);
                if (matcher.matches(properties)) {
                    for (Map.Entry<String, String> kv : data.entrySet()) {
                        item.setAttribute(kv.getKey(), kv.getValue());
                    }
                    return;
                }
            }
        }
    }

    public void deleteByMatcher(String nodeExpression, PropertiesMatcher matcher) {
        NodeList nodeList = (NodeList) evaluate(nodeExpression, doc, XPathConstants.NODESET);
        if (nodeList != null) {
            int length = nodeList.getLength();
            for (int i = 0; i < length; i++) {
                Element item = (Element) nodeList.item(i);
                Map<String, String> properties = getPropertiesWithContent(item, null);
                if (matcher.matches(properties)) {
                    deleteNode(item);
                    return;
                }
            }
        }
    }

    public void deleteAll(String nodeExpression) {
        NodeList nodeList = (NodeList) evaluate(nodeExpression, doc, XPathConstants.NODESET);
        if (nodeList != null) {
            int length = nodeList.getLength();
            for (int i = 0; i < length; i++) {
                Element item = (Element) nodeList.item(i);
                deleteNode(item);
            }
        }
    }

    public void delete(String nodeExpression) {
        Element element = (Element) evaluate(nodeExpression, doc, XPathConstants.NODE);
        deleteNode(element);
    }

    public void addNew(String parentNodeExpression, String tagName, Map<String, String> properties) {
        addNewWithContent(parentNodeExpression, tagName, properties, null);
    }

    public void addOrUpdate(String root, String parent, String tagName, Map<String, String> properties) {
        if (!setAttributes(root + "/" + parent + "/" + tagName, properties)) {
            if (!this.isNodeExists(root + "/" + parent)) {
                this.addNew(root, parent, null);
            }
            addNew(root + "/" + parent, tagName, properties);
        }
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
        } catch (Throwable t) {
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

    private Map<String, String> getPropertiesWithContent(Node node, String textContentKey) {
        if (node == null) {
            return null;
        }

        Map<String, String> properties = new LinkedHashMap<>();
        NamedNodeMap namedNodeMap = node.getAttributes();
        int length = namedNodeMap.getLength();
        for (int i = 0; i < length; i++) {
            Node item = namedNodeMap.item(i);
            properties.put(item.getNodeName(), item.getNodeValue());
        }
        if (textContentKey != null) {
            String textContent = node.getTextContent();
            if (textContent != null) {
                properties.put(textContentKey, textContent.trim());
            }
        }

        return properties;
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

    public String getNodeTextContent(String nodeExpression) {
        Node node = (Node) evaluate(nodeExpression, doc, XPathConstants.NODE);
        if (node == null) return null;
        return node.getTextContent();
    }

    public void addOrUpdateNode(String parent, String tagName, Map<String, String> map, boolean isList) {
        if (isList) {
            if (!isNodeExists(parent + "/" + tagName + "s")) {
                addNew(parent, tagName + "s", null);
            }
            addNew(parent + "/" + tagName + "s", tagName, map);
        } else {
            if (isNodeExists(parent + "/" + tagName)) {
                setAttributes(parent + "/" + tagName, map);
            } else {
                addNew(parent, tagName, map);
            }
        }
    }


    public interface PropertiesMatcher {
        boolean matchOne();

        boolean matches(Map<String, String> check);
    }
}
