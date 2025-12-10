package dev.scx.format.xml;

import dev.scx.node.DoubleNode;
import dev.scx.node.Node;
import dev.scx.node.NullNode;
import org.codehaus.stax2.XMLStreamReader2;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;

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

    public static Node deserialize(XMLStreamReader2 reader) throws XMLStreamException {
        // 1, 循环直到找到第一个元素起始
        while (true) {
            int eventType = reader.getEventType();
            if (eventType == XMLStreamConstants.START_ELEMENT) {
                break;
            }
            reader.next();
        }
        // 2, 解析
        var stack = new ContainerStack();
//        var node = _deserializeContainerNoRecursion(reader, stack, new ArrayList<>());
        Object o = _deserializeElement(reader);
        System.out.println();
        // 3, 验证是否存在后续多余内容
        while (reader.hasNext()) {
            // 非法内容 Woodstox 会为直接抛异常 无需我们处理
            reader.next();
        }

        // todo 这里需要转换
        return new DoubleNode(1);
    }

    private static Object _deserializeElement(XMLStreamReader2 reader) throws XMLStreamException {
        // 记录出现过的子元素和属性
        var elements = new ArrayList<Object>();

        // 1, 处理当前元素的属性
        int attributeCount = reader.getAttributeCount();
        for (int i = 0; i < attributeCount; i = i + 1) {
            var name = reader.getAttributeLocalName(i);
            var value = reader.getAttributeValue(i);
            elements.add(new AbstractMap.SimpleEntry<>(name, value));
        }

        // 2, 判断是否是自闭合标签
        var emptyElement = reader.isEmptyElement();
        // 自闭合标签 无需处理内部元素 直接返回
        if (emptyElement) {
            // 这里别忘了移动
            reader.next();
            if (elements.isEmpty()) {
                return NullNode.NULL;
            } else {
                return elements;
            }
        }

        while (true) {
            var eventType = reader.next();
            // 如果又遇到了一个 ELEMENT 进行递归解析
            if (eventType == XMLStreamConstants.START_ELEMENT) {
                var name = reader.getLocalName();
                var element = _deserializeElement(reader);
                elements.add(new AbstractMap.SimpleEntry<>(name, element));
            } else if (eventType == XMLStreamConstants.CHARACTERS) {
                // 遇到了文本 进行存储
                var text = reader.getText();
                // 忽略空白字符
                if (!text.isBlank()) {
                    elements.add(text);
                }
            } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                // 跳出循环
                break;
            }
            // 其余的 我们全部 当做不存在, 比如注释之类
        }

        return elements;
    }

    // Non-recursive alternative
    private static List<Object> _deserializeContainerNoRecursion(XMLStreamReader2 p, ContainerStack stack, final List<Object> root) throws  XMLStreamException {
        List<Object> curr = root;

        outer_loop:
        do {

            // 1, 处理当前元素的属性
            int attributeCount = p.getAttributeCount();
            for (int i = 0; i < attributeCount; i = i + 1) {
                var name = p.getAttributeLocalName(i);
                var value = p.getAttributeValue(i);
                curr.add(new AbstractMap.SimpleEntry<>(name, value));
            }

//            // 2, 判断是否是自闭合标签
            var emptyElement = p.isEmptyElement();
//            // 自闭合标签 无需处理内部元素 直接返回
            if (emptyElement) {
//                // 这里别忘了移动
                p.next();
//                if (attributeCount == 0) {
//                    return NullNode.NULL;
//                } else {
//                    return elements;
//                }
            }

            arrayLoop:
            while (true) {
                String value;
                var t = p.next();

                switch (t) {
                    case XMLStreamConstants.START_ELEMENT -> {
                        stack.push(curr);
                        var arr = new ArrayList<>();
                        var name = p.getLocalName();
                        curr.add(new AbstractMap.SimpleEntry<>(name, arr));
                        curr = arr;
                        continue outer_loop;
                    }
                    case END_ELEMENT -> {
                        break arrayLoop;
                    }
                    case XMLStreamConstants.CHARACTERS -> {
                        // 遇到了文本 进行存储
                        var text = p.getText();
                        // 忽略空白字符
                        if (!text.isBlank()) {
                            value = text;
                        } else {
                            value = null;
                        }

                    }
                    default -> throw new RuntimeException("123");
                }
                if (value != null) {
                    curr.add(value);
                }
                // Reached end of array (or input), so...


            }
            // Either way, Object or Array ended, return up nesting level:
            curr = stack.popOrNull();
        }
        while (curr != null);

        return root;
    }

}
