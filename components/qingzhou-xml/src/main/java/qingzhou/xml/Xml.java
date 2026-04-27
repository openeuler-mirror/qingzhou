package qingzhou.xml;

import java.io.File;
import java.io.InputStream;

public interface Xml {
    Doc parse(File file) throws Exception;

    Doc parse(InputStream is) throws Exception;
}
