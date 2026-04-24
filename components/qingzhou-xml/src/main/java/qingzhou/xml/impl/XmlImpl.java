package qingzhou.xml.impl;

import java.io.File;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import qingzhou.xml.Doc;
import qingzhou.xml.Xml;

@Component
public class XmlImpl implements Xml {
    private static DocumentBuilder builder;
    private static XPath xPath;

    @Activate
    public void init() throws Exception {
        DocumentBuilderFactory dbf;
        try {
            dbf = DocumentBuilderFactory.newInstance("com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl", null);
        } catch (Exception e) {
            dbf = DocumentBuilderFactory.newInstance();
        }
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        builder = dbf.newDocumentBuilder();

        XPathFactory factory = XPathFactory.newInstance();
        xPath = factory.newXPath();
    }

    @Override
    public Doc parse(File file) throws Exception {
        if (file == null) throw new IllegalArgumentException("File cannot be null");
        if (!file.isFile()) throw new IllegalArgumentException("Must be a file");

        org.w3c.dom.Document domDocument = builder.parse(file);
        return new DocImpl(domDocument, xPath);
    }
}
