package dev.scx.format.xml;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import dev.scx.format.xml.element.Element;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.charset.Charset;

import static com.ctc.wstx.api.WstxInputProperties.*;
import static dev.scx.format.xml.AutoCloseableXMLStreamReader.wrapReader;
import static dev.scx.format.xml.AutoCloseableXMLStreamWriter.wrapWriter;
import static dev.scx.format.xml.XmlDeserializer.deserialize;

/// XmlElementConverter
///
/// @author scx567888
/// @version 0.0.1
public final class XmlElementConverter {

    public XmlElementConverter() {

    }

    private static WstxInputFactory createWstxInputFactory(XmlNodeConverterOptions options) {
        // 这里我们使用 WstxInputFactory, 因为默认 XMLInputFactory 功能过于羸弱
        var xmlInputFactory = new WstxInputFactory();
        //有很多的 安全限制 Woodstox  已经覆盖了 我们直接使用
        xmlInputFactory.setProperty(P_MAX_ELEMENT_DEPTH, options.maxNestingDepth());
        xmlInputFactory.setProperty(P_MAX_TEXT_LENGTH, options.maxStringLength());
        xmlInputFactory.setProperty(P_MAX_ATTRIBUTE_SIZE, options.maxStringLength());
        xmlInputFactory.setProperty(P_MAX_CHILDREN_PER_ELEMENT, options.maxChildCount());
        xmlInputFactory.setProperty(P_MAX_ATTRIBUTES_PER_ELEMENT, options.maxChildCount());
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return xmlInputFactory;
    }

    private static AutoCloseableXMLStreamReader createXMLStreamReader(Reader reader, XmlNodeConverterOptions options) throws XMLStreamException {
        var xmlInputFactory = createWstxInputFactory(options);
        return wrapReader(xmlInputFactory.createXMLStreamReader(reader));
    }

    private static AutoCloseableXMLStreamReader createXMLStreamReader(InputStream inputStream, String enc, XmlNodeConverterOptions options) throws XMLStreamException {
        var xmlInputFactory = createWstxInputFactory(options);
        return wrapReader(xmlInputFactory.createXMLStreamReader(inputStream, enc));
    }

    private static AutoCloseableXMLStreamReader createXMLStreamReader(File file, XmlNodeConverterOptions options) throws XMLStreamException {
        var xmlInputFactory = createWstxInputFactory(options);
        return wrapReader(xmlInputFactory.createXMLStreamReader(file));
    }

    private static AutoCloseableXMLStreamWriter createXMLStreamWriter(Writer writer, XmlNodeConverterOptions options) throws XMLStreamException {
        var xmlOutputFactory = new WstxOutputFactory();
        return wrapWriter(xmlOutputFactory.createXMLStreamWriter(writer));
    }

    private static AutoCloseableXMLStreamWriter createXMLStreamWriter(OutputStream outputStream, String enc, XmlNodeConverterOptions options) throws XMLStreamException {
        var xmlOutputFactory = new WstxOutputFactory();
        return wrapWriter(xmlOutputFactory.createXMLStreamWriter(outputStream, enc));
    }

    public Element formatToElement(Reader reader, XmlNodeConverterOptions options) throws IOException, XMLStreamException {
        try (var xmlStreamReader = createXMLStreamReader(reader, options)) {
            return deserialize(xmlStreamReader.reader());
        }
    }

    public Element formatToElement(InputStream inputStream, Charset charset, XmlNodeConverterOptions options) throws IOException, XMLStreamException {
        try (var xmlStreamReader = createXMLStreamReader(inputStream, charset.name(), options)) {
            return deserialize(xmlStreamReader.reader());
        }
    }

    public Element formatToElement(String text, XmlNodeConverterOptions options) throws XMLStreamException, IOException {
        try (var reader = new StringReader(text)) {
            return formatToElement(reader, options);
        }
    }

    public Element formatToElement(byte[] bytes, Charset charset, XmlNodeConverterOptions options) throws XMLStreamException, IOException {
        try (var inputStream = new ByteArrayInputStream(bytes)) {
            return formatToElement(inputStream, charset, options);
        }
    }

    public Element formatToElement(File file, Charset charset, XmlNodeConverterOptions options) throws IOException, XMLStreamException {
        try (var xmlStreamReader = createXMLStreamReader(file, options)) {
            return deserialize(xmlStreamReader.reader());
        }
    }

    public void elementToFormat(Element element, Writer writer, XmlNodeConverterOptions options) throws IOException, XMLStreamException {
        try (var xmlStreamWriter = createXMLStreamWriter(writer, options)) {
            new XmlSerializer(options).serialize(xmlStreamWriter.writer(), element);
        }
    }

    public void elementToFormat(Element element, OutputStream outputStream, Charset charset, XmlNodeConverterOptions options) throws IOException, XMLStreamException {
        try (var xmlStreamWriter = createXMLStreamWriter(outputStream, charset.name(), options)) {
            new XmlSerializer(options).serialize(xmlStreamWriter.writer(), element);
        }
    }

    public String elementToFormatString(Element element, XmlNodeConverterOptions options) throws IOException, XMLStreamException {
        try (var writer = new StringWriter()) {
            elementToFormat(element, writer, options);
            return writer.toString();
        }
    }

    public byte[] elementToFormatBytes(Element element, Charset charset, XmlNodeConverterOptions options) throws IOException, XMLStreamException {
        try (var outputStream = new ByteArrayOutputStream()) {
            elementToFormat(element, outputStream, charset, options);
            return outputStream.toByteArray();
        }
    }

    public File elementToFormatFile(Element element, File file, Charset charset, XmlNodeConverterOptions options) throws IOException, XMLStreamException {
        try (var outputStream = new FileOutputStream(file)) {
            elementToFormat(element, outputStream, charset, options);
            return file;
        }
    }

}
