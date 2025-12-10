package dev.scx.format.xml;

import dev.scx.format.xml.element.Element;
import dev.scx.format.xml.element.TagElement;
import dev.scx.format.xml.element.TextElement;
import org.codehaus.stax2.XMLStreamReader2;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static javax.xml.stream.XMLStreamConstants.*;

/// ### 解析规则:
///
/// 1. `<root></root>` -> `""`
///     没有文本和子元素 -> StringNode("")
///
/// 2. `<root/>` -> `NULL`
///     没有文本和子元素 (自闭合标签) -> NULL
///
/// 3. `<root>123</root>` -> `"123"`
///     只有文本时 -> StringNode
///
/// 4. `<root><b>123</b></root>` -> `{"b": "123"}`
///     存在单个子元素 -> ObjectNode
///
/// 5. `<root name="jack"></root>` 或 `<root name="jack" />` -> `{"name": "jack"}`
///     存在单个属性 (无论是否自闭合) -> ObjectNode
///
/// 6. `<root name="jack"><age>18</age></root>` -> `{"name": "jack", "age": "18"}`
///     存在单个子元素, 属性和子元素相同方式看待 -> ObjectNode
///
/// 7. `<root>000<b>123</b></root>` -> `["000",{"b": "123"}]`
///     同时存在子元素和文本 -> 转换为数组 ArrayNode
///
/// 8. `<root>000<b>123</b>6666</root>` -> `["000",{"b": "123"}, "6666"]`
///     同时存在子元素和多个文本 -> 转换为数组 ArrayNode
///
/// 9. `<root><b>123</b><a>999</a><b>456</b></root>` -> `[{"b": "123"},{"a": "999"},{"b": "456"}]`
///     存在多个子元素(无论是否同名) -> 转换为数组 ArrayNode
///
/// 10, `<root name="jack" age="22" name="rose"></root>` -> `[{"name": "jack"},{"age": "22"},{"name": "rose"}]`
///     存在多个属性(无论是否同名) -> 转换为数组 ArrayNode
///
/// 11, `<root name="">  <b> 1 2 3 </b>   </root>` -> `[{"name": ""},{"b": " 1 2 3 "}]`
///     所有的纯空白文本节点视为不存在, 但有内容则保留原始文本, 属性永远保留原始文本
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
        return _deserializeElementNoRecursion(reader, stack, new TagElement(reader.getLocalName()));
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
                        var newElement = new TagElement(p.getLocalName());
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
            // Reached end of array (or input), so...

            // Either way, Object or Array ended, return up nesting level:
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
