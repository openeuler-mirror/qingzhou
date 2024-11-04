package qingzhou.uml.impl;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import qingzhou.uml.Uml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

class UmlImpl implements Uml {
    @Override
    public byte[] toSvg(String source) throws IOException {
        SourceStringReader reader = new SourceStringReader(source);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        reader.outputImage(os, new FileFormatOption(FileFormat.SVG));
        return os.toByteArray();
    }
}
