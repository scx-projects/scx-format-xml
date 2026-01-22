package dev.scx.format.xml;

import dev.scx.format.xml.element.Element;
import dev.scx.format.xml.element.TagElement;
import dev.scx.format.xml.element.TextElement;
import org.codehaus.stax2.XMLStreamReader2;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static javax.xml.stream.XMLStreamConstants.*;

/// XmlDeserializer
///
/// @author scx567888
/// @version 0.0.1
final class XmlDeserializer {

    public static Element deserialize(XMLStreamReader2 reader) throws XMLStreamException {
        // 1, 循环直到找到第一个元素起始
        while (reader.getEventType() != START_ELEMENT) {
            reader.next();
        }
        // 2, 解析为 element 结构
        var element = _deserializeElement(reader);
        // 3, 验证是否存在后续多余内容
        while (reader.hasNext()) {
            // 非法内容 Woodstox 会为直接抛异常 无需我们处理
            reader.next();
        }
        return element;
    }

    private static Element _deserializeElement(XMLStreamReader2 reader) throws XMLStreamException {
        var stack = new TagElementStack();
        var root = new TagElement(reader.getLocalName(), reader.isEmptyElement());
        _deserializeAttribute(reader, root);
        return _deserializeElementNoRecursion(reader, stack, root);
    }

    private static Element _deserializeElementNoRecursion(XMLStreamReader2 p, TagElementStack stack, final TagElement root) throws XMLStreamException {
        TagElement curr = root;

        outer_loop:
        do {
            var currElement = curr;

            elementLoop:
            while (true) {
                Element value;
                var t = p.next();
                switch (t) {
                    case START_ELEMENT -> {
                        var newElement = new TagElement(p.getLocalName(), p.isEmptyElement());
                        _deserializeAttribute(p, newElement);
                        currElement.add(newElement);
                        stack.push(curr);
                        curr = newElement;
                        continue outer_loop;
                    }
                    case END_ELEMENT -> {
                        break elementLoop;
                    }
                    case CHARACTERS -> value = _fromText(p);
                    default -> value = null;// 忽略其他所有情况
                }
                if (value != null) {
                    currElement.add(value);
                }
            }

            curr = stack.popOrNull();
        } while (curr != null);

        return root;
    }

    private static void _deserializeAttribute(XMLStreamReader2 p, TagElement tagElement) {
        // 处理属性
        for (int i = 0; i < p.getAttributeCount(); i++) {
            var n = p.getAttributeLocalName(i);
            var v = p.getAttributeValue(i);
            tagElement.addAttribute(n, v);
        }
    }

    private static TextElement _fromText(XMLStreamReader p) {
        var text = p.getText();
        return text.isBlank() ? null : new TextElement(text);
    }

}
