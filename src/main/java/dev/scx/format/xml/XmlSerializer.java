package dev.scx.format.xml;

import dev.scx.format.NodeToFormatException;
import dev.scx.format.xml.element.Element;
import dev.scx.format.xml.element.TagElement;
import dev.scx.format.xml.element.TextElement;
import org.codehaus.stax2.XMLStreamWriter2;

import javax.xml.stream.XMLStreamException;

/// ### 序列化规则
///
/// 0. 跟标签默认 -> root, 没有上下文 key 可用的数组 默认 -> item.
///    支持外部配置, 防止某些情况下的冲突
///
/// 1. `"123"` -> `<root>123</root>`
///    值类型 -> 标准标签
///
/// 2. `NULL` -> `<root/>`
///    NULL -> 闭合标签
///
/// 3. `{"a": 123}` -> `<root><a>123</a></root>`
///    对象类型 -> 嵌套标签
///
/// 4. `{"a": [1, 2]}` -> `<root><a>1</a><a>2</a></root>``
///    数组 -> 尝试重复
///
/// 5. `[1, 2]` -> `<root><item>1</item><item>2</item></root>`
///    数组没有可用的上文 key -> 使用 item
///
/// 6. `[1, [2]]` -> `<root><item>1</item><item><item>2</item></item></root>`
///    嵌套数组不进行任何扁平化
///
/// 7, `{"": 123}` -> `<root>123</root>`
///    key 为 "", 直接解包
///
/// @author scx567888
/// @version 0.0.1
final class XmlSerializer {

    private final int maxNestingDepth;

    public XmlSerializer(XmlNodeConverterOptions options) {
        this.maxNestingDepth = options.maxNestingDepth();
    }

    public void serialize(XMLStreamWriter2 writer2, Element element) throws XMLStreamException, NodeToFormatException {
        _serialize(writer2, element, 1);
    }

    private void _serialize(XMLStreamWriter2 writer2, Element element, int currentDepth) throws XMLStreamException, NodeToFormatException {
        if (currentDepth > maxNestingDepth) {
            throw new NodeToFormatException("Nesting depth exceeds limit: " + maxNestingDepth);
        }
        switch (element) {
            case TagElement tagElement -> {
                // 没有子元素 使用自闭合标签
                if (tagElement.isEmpty()) {
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
