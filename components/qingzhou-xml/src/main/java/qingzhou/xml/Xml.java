package qingzhou.xml;

import java.io.File;

public interface Xml {
    Doc parse(File file) throws Exception;
}
