package qingzhou.xml;

import java.io.File;
import java.util.Properties;

public interface Xml {
    Doc parse(File file) throws Exception;
}
