package qingzhou.xml.impl;

import java.io.File;
import java.io.InputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import qingzhou.xml.Doc;
import qingzhou.xml.Xml;

@Component
public class XmlImpl implements Xml {
    private DocumentBuilderFactory factory;

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
        factory = dbf;
    }

    @Override
    public Doc parse(File file) throws Exception {
        if (file == null) throw new IllegalArgumentException("File cannot be null");
        if (!file.isFile()) throw new IllegalArgumentException("Must be a file");

        return new DocImpl(newBuilder().parse(file));
    }

    @Override
    public Doc parse(InputStream is) throws Exception {
        if (is == null) throw new IllegalArgumentException("InputStream cannot be null");
        return new DocImpl(newBuilder().parse(is));
    }

    /**
     * DocumentBuilder 非线程安全，每次解析新建；加解密等安全特性由 init 配置的 factory 统一保证。
     */
    private DocumentBuilder newBuilder() throws Exception {
        return factory.newDocumentBuilder();
    }
}
