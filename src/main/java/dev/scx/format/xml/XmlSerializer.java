package dev.scx.format.xml;

import dev.scx.format.NodeToFormatException;
import dev.scx.format.xml.element.Element;
import dev.scx.format.xml.element.TagElement;
import dev.scx.format.xml.element.TextElement;
import org.codehaus.stax2.XMLStreamWriter2;

import javax.xml.stream.XMLStreamException;

/// ### 关于序列化
/// 此序列化器基于递归下降方式进行序列化, 以保证代码的简洁和可维护性.
/// 但 Element 实际上允许自引用, 也就是说存在无限递归导致栈溢出的风险.
/// 因此, 我们通过 [XmlNodeConvertOptions#maxNestingDepth(int)] 来间接限制递归深度,
/// 避免超过 JVM 栈限制 (一般超过 3500 层为危险值)
///
/// @author scx567888
final class XmlSerializer {

    private final int maxNestingDepth;
    private final String rootName;

    public XmlSerializer(XmlNodeConvertOptions options) {
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
