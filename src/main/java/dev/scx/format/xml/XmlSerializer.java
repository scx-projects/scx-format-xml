package dev.scx.format.xml;

import dev.scx.format.NodeToFormatException;
import dev.scx.format.xml.element.Element;
import dev.scx.format.xml.element.TagElement;
import dev.scx.format.xml.element.TextElement;
import org.codehaus.stax2.XMLStreamWriter2;

import javax.xml.stream.XMLStreamException;

/// XmlSerializer
///
/// @author scx567888
/// @version 0.0.1
final class XmlSerializer {

    private final int maxNestingDepth;
    private final String rootName;

    public XmlSerializer(XmlNodeConverterOptions options) {
        this.maxNestingDepth = options.maxNestingDepth();
        this.rootName = options.rootName();
    }

    public void serialize(XMLStreamWriter2 writer2, Element element) throws XMLStreamException, NodeToFormatException {
        // 我们需要尝试包裹独立的 标签
        if (element instanceof TextElement) {
            var root = new TagElement(rootName, false);
            root.add(element);
            _serialize(writer2, root, 1);
        } else {
            _serialize(writer2, element, 1);
        }
    }

    private void _serialize(XMLStreamWriter2 writer2, Element element, int currentDepth) throws XMLStreamException, NodeToFormatException {
        if (currentDepth > maxNestingDepth) {
            throw new NodeToFormatException("Nesting depth exceeds limit: " + maxNestingDepth);
        }
        switch (element) {
            case TagElement tagElement -> {
                // 没有子元素 使用自闭合标签
                if (tagElement.isEmpty() && tagElement.useSelfClosing()) {
                    writer2.writeEmptyElement(tagElement.tagName());
                    for (var attribute : tagElement.attributes()) {
                        writer2.writeAttribute(attribute.name(), attribute.value());
                    }
                    return;
                }

                // 标准标签
                writer2.writeStartElement(tagElement.tagName());
                for (var attribute : tagElement.attributes()) {
                    writer2.writeAttribute(attribute.name(), attribute.value());
                }
                for (var e : tagElement) {
                    _serialize(writer2, e, currentDepth + 1);
                }
                writer2.writeEndElement();
            }
            case TextElement textElement -> {
                writer2.writeCharacters(textElement.text());
            }
        }
    }

}
