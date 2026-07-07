package dev.scx.format.xml.test;

import dev.scx.format.FormatToNodeException;
import dev.scx.format.NodeToFormatException;
import dev.scx.format.xml.ElementNodeConverter;
import dev.scx.format.xml.XmlNodeConvertConfig;
import dev.scx.format.xml.element.Element;
import dev.scx.node.ArrayNode;
import dev.scx.node.Node;
import dev.scx.node.ObjectNode;
import dev.scx.node.StringNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import static dev.scx.format.xml.XmlNodeConverter.DEFAULT_XML_NODE_CONVERTER;
import static dev.scx.node.BooleanNode.FALSE;
import static dev.scx.node.BooleanNode.TRUE;
import static dev.scx.node.NullNode.NULL;

public class ElementNodeConverterTest {

    public static void main(String[] args) throws XMLStreamException, IOException {
        test1();
        test2();
        test3();
        test4();
        test5();
        test6();
        test7();
        test8();
        test9();
        test10();
        test11();
        test12();
        test13();
        test14();
        test15();
        test16();
        test17();
        test18();
        test19();
        test20();
        test21();
        test22();
        test23();
        test24();
        test25();
        test26();
        test27();
        test28();
        test29();
        test30();
        test31();
        test32();
        test33();
        test34();
    }

    /// <a></a> -> ""
    @Test
    public static void test1() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <a></a>
            """);
        var node1 = toNode(element1);
        Assert.assertEquals(node1.toString(), "\"\"");
    }

    /// 重复标签 + 自闭合标签
    /// <root><b>1</b><b/><b>2</b></root>
    /// -> {"b":["1",null,"2"]}
    /// -> 还原回 Element
    @Test
    public static void test2() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <root><b>1</b><b/><b>2</b></root>
            """);
        var node1 = toNode(element1);
        var element2 = toElement(node1);
        Assert.assertEquals(element1, element2);
    }

    /// 顶层数组 -> <root><item>...</item></root>
    @Test
    public static void test3() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <root><item>1</item><item/><item>2</item></root>
            """);
        var node1 = new ArrayNode();
        node1.add(1);
        node1.add(NULL);
        node1.add(2);
        var element2 = toElement(node1);
        Assert.assertEquals(element1, element2);
    }

    /// mixed content 必须拒绝
    @Test
    public static void test4() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <root>
                hello
                <a>1</a>
            </root>
            """);

        Assert.expectThrows(FormatToNodeException.class, () -> {
            toNode(element1);
        });
    }

    /// 属性参与 ObjectNode 构建
    /// <root name="jack"><age>18</age></root>
    /// -> {"name":"jack","age":"18"}
    @Test
    public static void test5() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <root name="jack">
                <age>18</age>
            </root>
            """);

        var node1 = toNode(element1);

        var expected = new ObjectNode();
        expected.put("name", "jack");
        expected.put("age", "18");

        Assert.assertEquals(node1, expected);
    }

    /// 属性来源不会保留, Node -> Element 时统一转成子元素
    /// <root name="jack"/> -> {"name":"jack"} -> <root><name>jack</name></root>
    @Test
    public static void test6() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <root name="jack"/>
            """);

        var node1 = toNode(element1);
        var element2 = toElement(node1);

        var expected = toElement("""
            <root><name>jack</name></root>
            """);

        Assert.assertEquals(element2, expected);
    }

    /// Node 经由 XML 往返后，非 null 标量会字符串化
    @Test
    public static void test7() throws XMLStreamException, IOException {
        var node1 = new ObjectNode();
        node1.put("i", 123);
        node1.put("l", 1234567890123L);
        node1.put("f", 1.25f);
        node1.put("d", 9.99d);
        node1.put("t", TRUE);
        node1.put("f2", FALSE);
        node1.put("n", NULL);

        var element1 = toElement(node1);
        var node2 = toNode(element1);

        var expected = new ObjectNode();
        expected.put("i", "123");
        expected.put("l", "1234567890123");
        expected.put("f", "1.25");
        expected.put("d", "9.99");
        expected.put("t", "true");
        expected.put("f2", "false");
        expected.put("n", NULL);

        Assert.assertEquals(node2, expected);
    }

    /// 嵌套数组不扁平化
    /// [1, [2]] -> <root><item>1</item><item><item>2</item></item></root>
    @Test
    public static void test8() throws XMLStreamException, IOException {
        var node1 = new ArrayNode();
        node1.add(1);

        var nested = new ArrayNode();
        nested.add(2);
        node1.add(nested);

        var element1 = toElement(node1);

        var expectedElement = toElement("""
            <root>
                <item>1</item>
                <item>
                    <item>2</item>
                </item>
            </root>
            """);

        Assert.assertEquals(element1, expectedElement);

        var node2 = toNode(element1);
        var element2 = toElement(node2);

        // 不要求 node2 == node1
        // 只要求规范化后再次输出稳定
        Assert.assertEquals(element1, element2);
    }

    /// 同名标签允许异构数组
    /// <root><x>123</x><x/><x><y>1</y></x></root>
    /// -> {"x":["123",null,{"y":"1"}]}
    @Test
    public static void test9() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <root>
                <x>123</x>
                <x/>
                <x><y>1</y></x>
            </root>
            """);

        var node1 = toNode(element1);

        var expected = new ObjectNode();
        var arr = new ArrayNode();
        arr.add("123");
        arr.add(NULL);

        var obj = new ObjectNode();
        obj.put("y", "1");
        arr.add(obj);

        expected.put("x", arr);

        Assert.assertEquals(node1, expected);

        var element2 = toElement(node1);
        Assert.assertEquals(element1, element2);
    }

    /// 空字符串字段名非法
    @Test
    public static void test10() {
        var node1 = new ObjectNode();
        node1.put("", "123");

        Assert.expectThrows(NodeToFormatException.class, () -> {
            toElement(node1);
        });
    }


    /// <a/> -> NULL
    @Test
    public static void test11() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <a/>
            """);
        var node1 = toNode(element1);
        Assert.assertEquals(node1, NULL);
    }

    /// <a>   </a> -> ""
    @Test
    public static void test12() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <a>   </a>
            """);
        var node1 = toNode(element1);
        Assert.assertEquals(node1.toString(), "\"\"");
    }

    /// <a><![CDATA[]]></a> -> ""
    @Test
    public static void test13() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <a><![CDATA[]]></a>
            """);
        var node1 = toNode(element1);
        Assert.assertEquals(node1.toString(), "\"\"");
    }

    /// <a x="1"></a> -> {"x":"1"}
    @Test
    public static void test14() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <a x="1"></a>
            """);
        var node1 = toNode(element1);

        var expected = new ObjectNode();
        expected.put("x", "1");

        Assert.assertEquals(node1, expected);
    }

    /// <a x="1">   </a> -> {"x":"1"}
    @Test
    public static void test15() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <a x="1">   </a>
            """);
        var node1 = toNode(element1);

        var expected = new ObjectNode();
        expected.put("x", "1");

        Assert.assertEquals(node1, expected);
    }

    /// mixed content: text + child
    @Test
    public static void test16() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <a>text<b>1</b></a>
            """);

        Assert.expectThrows(FormatToNodeException.class, () -> {
            toNode(element1);
        });
    }

    /// mixed content: child + text
    @Test
    public static void test17() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <a><b>1</b>text</a>
            """);

        Assert.expectThrows(FormatToNodeException.class, () -> {
            toNode(element1);
        });
    }

    /// mixed content: text + attr
    @Test
    public static void test18() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <a x="1">text</a>
            """);

        Assert.expectThrows(FormatToNodeException.class, () -> {
            toNode(element1);
        });
    }

    /// attr + child same name -> merge array
    @Test
    public static void test19() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <a x="1"><x>2</x></a>
            """);
        var node1 = toNode(element1);

        var expected = new ObjectNode();
        var arr = new ArrayNode();
        arr.add("1");
        arr.add("2");
        expected.put("x", arr);

        Assert.assertEquals(node1, expected);
    }

    /// repeated child merge
    @Test
    public static void test20() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <a><x>1</x><x>2</x><x>3</x></a>
            """);
        var node1 = toNode(element1);

        var expected = new ObjectNode();
        var arr = new ArrayNode();
        arr.add("1");
        arr.add("2");
        arr.add("3");
        expected.put("x", arr);

        Assert.assertEquals(node1, expected);
    }

    /// empty object -> <root></root> -> {}
    @Test
    public static void test21() throws XMLStreamException, IOException {
        var node1 = new ObjectNode();

        var element1 = toElement(node1);
        var expectedElement = toElement("""
            <root></root>
            """);
        Assert.assertEquals(element1, expectedElement);

        var node2 = toNode(element1);
        Assert.assertEquals(node2, new StringNode(""));
    }

    /// {"a":{}} -> <root><a></a></root> -> {"a":""}
    /// 这里用于确认空对象经 XML 往返后会退化
    @Test
    public static void test22() throws XMLStreamException, IOException {
        var node1 = new ObjectNode();
        node1.put("a", new ObjectNode());

        var element1 = toElement(node1);
        var expectedElement = toElement("""
            <root><a></a></root>
            """);
        Assert.assertEquals(element1, expectedElement);

        var node2 = toNode(element1);

        var expectedNode = new ObjectNode();
        expectedNode.put("a", "");

        Assert.assertEquals(node2, expectedNode);
    }

    /// {"a":null,"b":"","c":{}} 的规范化结果确认
    @Test
    public static void test23() throws XMLStreamException, IOException {
        var node1 = new ObjectNode();
        node1.put("a", NULL);
        node1.put("b", "");
        node1.put("c", new ObjectNode());

        var element1 = toElement(node1);
        var node2 = toNode(element1);

        var expectedNode = new ObjectNode();
        expectedNode.put("a", NULL);
        expectedNode.put("b", "");
        expectedNode.put("c", "");

        Assert.assertEquals(node2, expectedNode);
    }

    /// 空数组 -> <root></root> -> ""
    /// 这里也是确认当前规范下空数组不可逆
    @Test
    public static void test24() throws XMLStreamException, IOException {
        var node1 = new ArrayNode();

        var element1 = toElement(node1);
        var expectedElement = toElement("""
            <root></root>
            """);
        Assert.assertEquals(element1, expectedElement);

        var node2 = toNode(element1);
        Assert.assertEquals(node2.toString(), "\"\"");
    }

    /// {"a":[]} -> <root></root> 不成立，当前实现实际会生成 <root></root> 内没有 a
    /// 用于确认空数组字段当前会被丢失
    @Test
    public static void test25() throws XMLStreamException, IOException {
        var node1 = new ObjectNode();
        node1.put("a", new ArrayNode());

        var element1 = toElement(node1);
        var expectedElement = toElement("""
            <root></root>
            """);

        Assert.assertEquals(element1, expectedElement);

        var node2 = toNode(element1);
        Assert.assertEquals(node2, new StringNode(""));
    }

    /// 顶层数组 round-trip 稳定
    @Test
    public static void test26() throws XMLStreamException, IOException {
        var node1 = new ArrayNode();
        node1.add("1");
        node1.add("2");
        node1.add(NULL);

        var element1 = toElement(node1);
        var node2 = toNode(element1);
        var element2 = toElement(node2);

        Assert.assertEquals(element1, element2);
    }

    /// 对象字段数组 round-trip 稳定
    @Test
    public static void test27() throws XMLStreamException, IOException {
        var node1 = new ObjectNode();
        var arr = new ArrayNode();
        arr.add("1");
        arr.add("2");
        arr.add(NULL);
        node1.put("a", arr);

        var element1 = toElement(node1);
        var node2 = toNode(element1);
        var element2 = toElement(node2);

        Assert.assertEquals(element1, element2);
    }

    /// 标量字符串化: Int/Long/Float/Double/BigInteger/BigDecimal/Boolean
    @Test
    public static void test28() throws XMLStreamException, IOException {
        var node1 = new ObjectNode();
        node1.put("i", 123);
        node1.put("l", 1234567890123L);
        node1.put("f", 1.25f);
        node1.put("d", 9.99d);
        node1.put("bi", new BigInteger("12345678901234567890"));
        node1.put("bd", new BigDecimal("12345.6789"));
        node1.put("t", true);
        node1.put("f2", false);
        node1.put("n", NULL);

        var node2 = toNode(toElement(node1));

        var expected = new ObjectNode();
        expected.put("i", "123");
        expected.put("l", "1234567890123");
        expected.put("f", "1.25");
        expected.put("d", "9.99");
        expected.put("bi", "12345678901234567890");
        expected.put("bd", "12345.6789");
        expected.put("t", "true");
        expected.put("f2", "false");
        expected.put("n", NULL);

        Assert.assertEquals(node2, expected);
    }

    /// 特殊文本字符转义稳定性
    @Test
    public static void test29() throws XMLStreamException, IOException {
        var xml = """
            <root>&lt;tag&gt;&amp;"'</root>
            """;

        var element1 = toElement(xml);
        var node1 = toNode(element1);
        var element2 = toElement(node1);

        Assert.assertEquals(element1, element2);
    }

    /// CDATA 作为普通文本处理
    @Test
    public static void test30() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <root><![CDATA[<b>hi</b>]]></root>
            """);
        var node1 = toNode(element1);
        var element2 = toElement(node1);

        var expectedNode = new StringNode("<b>hi</b>");
        Assert.assertEquals(node1, expectedNode);
        Assert.assertEquals(element2, toElement("""
            <root>&lt;b&gt;hi&lt;/b&gt;</root>
            """));
    }

    /// 特殊字符: < > & " '
    @Test
    public static void test31() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <a>&lt;tag&gt;&amp;"'</a>
            """);

        var node1 = toNode(element1);
        Assert.assertEquals(node1, new StringNode("<tag>&\"'"));

        var element2 = toElement(node1);
        var node2 = toNode(element2);

        Assert.assertEquals(node1, node2);
    }

    /// Unicode / emoji 文本
    @Test
    public static void test32() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <a>明哥🚀你好🙂</a>
            """);

        var node1 = toNode(element1);
        Assert.assertEquals(node1, new StringNode("明哥🚀你好🙂"));

        var element2 = toElement(node1);
        var node2 = toNode(element2);

        Assert.assertEquals(node1, node2);
    }

    /// CDATA 文本应等价于普通文本
    @Test
    public static void test33() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <a><![CDATA[<b>hi</b>&hello]]></a>
            """);

        var node1 = toNode(element1);
        Assert.assertEquals(node1, new StringNode("<b>hi</b>&hello"));

        var element2 = toElement(node1);
        var node2 = toNode(element2);

        Assert.assertEquals(node1, node2);
    }

    /// 非纯空白换行文本应保留
    @Test
    public static void test34() throws XMLStreamException, IOException {
        var element1 = toElement("""
            <a>line1
            line2</a>
            """);

        var node1 = toNode(element1);
        Assert.assertEquals(node1, new StringNode("line1\nline2"));

        var element2 = toElement(node1);
        var node2 = toNode(element2);

        Assert.assertEquals(node1, node2);
    }

    public static Element toElement(String xml) throws XMLStreamException, IOException {
        return DEFAULT_XML_NODE_CONVERTER.xmlElementConverter().formatToElement(xml, XmlNodeConvertConfig.of());
    }

    public static Element toElement(Node node) {
        return new ElementNodeConverter(XmlNodeConvertConfig.of()).nodeToElement(node);
    }

    public static Node toNode(Element element) {
        return new ElementNodeConverter(XmlNodeConvertConfig.of()).elementToNode(element);
    }

}
