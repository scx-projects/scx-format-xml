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
/// 1. `<a></a>` -> `""`
///     没有文本和子元素 -> StringNode("")
///
/// 2. `<a/>` -> `NULL`
///     没有文本和子元素 (自闭合标签) -> NULL
///
/// 3. `<a>123</a>` -> `"123"`
///     只有文本时 -> StringNode
///
/// 4. `<a><b>123</b></a>` -> `{"b": "123"}`
///     存在子元素 -> ObjectNode
///
/// 5. `<a name="jack"></a>` 或 `<a name="jack" />` -> `{"name": "jack"}`
///     存在属性 (无论是否自闭合) -> ObjectNode
///
/// 6. `<a name="jack"><age>18</age></a>` -> `{"name": "jack", "age": "18"}`
///     属性和子元素相同方式看待 -> ObjectNode
///
/// 7. `<a>000<b>123</b></a>` -> `{"b": "123", "": "000"}`
///     同时存在子元素和单个文本 -> 将文本视为单个 StringNode, 并以 "" 为 key 合并到 ObjectNode 中
///
/// 8. `<a>000<b>123</b>6666</a>` -> `{"b": "123", "": ["000", "6666"]}`
///     同时存在子元素和多个文本 -> 将多个文本视为 ArrayNode(StringNode[]) , 并以 "" 为 key 合并到 Object 中
///
/// 9. `<a><b>123</b><b>456</b></a>` -> `{"b": ["123", "456"]}`
///     存在多个同名子元素 -> 合并子元素为 ArrayNode
///
/// 10, `<a name="jack" name="rose"></a>` -> `{"name": ["jack", "rose"]}`
///     存在多个同名属性 -> 合并属性为 ArrayNode
///
/// 11, `<a name="">  <b> 1 2 3 </b>   </a>` -> `{"b": " 1 2 3 ", "name": "" }`
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
        var root = new TagElement(reader.getLocalName());
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
