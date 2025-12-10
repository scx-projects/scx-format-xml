package dev.scx.format.xml;

import dev.scx.format.xml.element.Element;
import dev.scx.format.xml.element.TagElement;
import dev.scx.node.IntNode;
import dev.scx.node.Node;

// todo 采取什么规则 ?
public class ElementNodeConverter {

    /// ### elementToNode 规则:
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
    public static Node elementToNode(Element element) {

        return new IntNode(0);
    }

    /// ### nodeToElement 规则
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
    public static TagElement nodeToElement(Node node) {

        return new TagElement("root");
    }

}
