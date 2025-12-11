package dev.scx.format.xml;

import dev.scx.format.FormatNodeConverter;
import dev.scx.format.FormatToNodeException;
import dev.scx.format.NodeToFormatException;
import dev.scx.node.Node;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.charset.Charset;

/// XmlNodeConverter
///
/// @author scx567888
/// @version 0.0.1
public final class XmlNodeConverter implements FormatNodeConverter<XmlNodeConverterOptions> {

    private final XmlElementConverter xmlElementConverter;

    public XmlNodeConverter() {
        this.xmlElementConverter = new XmlElementConverter();
    }

    @Override
    public Node formatToNode(Reader reader, XmlNodeConverterOptions options) throws FormatToNodeException, IOException {
        try {
            var element = xmlElementConverter.formatToElement(reader, options);
            return new ElementNodeConverter(options).elementToNode(element);
        } catch (XMLStreamException e) {
            throw new FormatToNodeException(e);
        }
    }

    @Override
    public Node formatToNode(InputStream inputStream, Charset charset, XmlNodeConverterOptions options) throws FormatToNodeException, IOException {
        try {
            var element = xmlElementConverter.formatToElement(inputStream, charset, options);
            return new ElementNodeConverter(options).elementToNode(element);
        } catch (XMLStreamException e) {
            throw new FormatToNodeException(e);
        }
    }

    @Override
    public Node formatToNode(String text, XmlNodeConverterOptions options) throws FormatToNodeException {
        try {
            var element = xmlElementConverter.formatToElement(text, options);
            return new ElementNodeConverter(options).elementToNode(element);
        } catch (XMLStreamException | IOException e) {
            throw new FormatToNodeException(e);
        }
    }

    @Override
    public Node formatToNode(byte[] bytes, Charset charset, XmlNodeConverterOptions options) throws FormatToNodeException {
        try {
            var element = xmlElementConverter.formatToElement(bytes, charset, options);
            return new ElementNodeConverter(options).elementToNode(element);
        } catch (XMLStreamException | IOException e) {
            throw new FormatToNodeException(e);
        }
    }

    @Override
    public Node formatToNode(File file, Charset charset, XmlNodeConverterOptions options) throws FormatToNodeException, IOException {
        try {
            var element = xmlElementConverter.formatToElement(file, charset, options);
            return new ElementNodeConverter(options).elementToNode(element);
        } catch (XMLStreamException e) {
            throw new FormatToNodeException(e);
        }
    }

    @Override
    public void nodeToFormat(Node node, Writer writer, XmlNodeConverterOptions options) throws NodeToFormatException, IOException {
        try {
            var element = new ElementNodeConverter(options).nodeToElement(node);
            xmlElementConverter.elementToFormat(element, writer, options);
        } catch (XMLStreamException e) {
            throw new NodeToFormatException(e);
        }
    }

    @Override
    public void nodeToFormat(Node node, OutputStream outputStream, Charset charset, XmlNodeConverterOptions options) throws NodeToFormatException, IOException {
        try {
            var element = new ElementNodeConverter(options).nodeToElement(node);
            xmlElementConverter.elementToFormat(element, outputStream, charset, options);
        } catch (XMLStreamException e) {
            throw new NodeToFormatException(e);
        }
    }

    @Override
    public String nodeToFormatString(Node node, XmlNodeConverterOptions options) throws NodeToFormatException {
        try {
            var element = new ElementNodeConverter(options).nodeToElement(node);
            return xmlElementConverter.elementToFormatString(element, options);
        } catch (XMLStreamException | IOException e) {
            throw new NodeToFormatException(e);
        }
    }

    @Override
    public byte[] nodeToFormatBytes(Node node, Charset charset, XmlNodeConverterOptions options) throws NodeToFormatException {
        try {
            var element = new ElementNodeConverter(options).nodeToElement(node);
            return xmlElementConverter.elementToFormatBytes(element, charset, options);
        } catch (XMLStreamException | IOException e) {
            throw new NodeToFormatException(e);
        }
    }

    @Override
    public File nodeToFormatFile(Node node, File file, Charset charset, XmlNodeConverterOptions options) throws NodeToFormatException, IOException {
        try {
            var element = new ElementNodeConverter(options).nodeToElement(node);
            return xmlElementConverter.elementToFormatFile(element, file, charset, options);
        } catch (XMLStreamException e) {
            throw new NodeToFormatException(e);
        }
    }

    public XmlElementConverter xmlElementConverter() {
        return xmlElementConverter;
    }

}
