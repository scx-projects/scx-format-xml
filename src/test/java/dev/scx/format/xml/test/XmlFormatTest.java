package dev.scx.format.xml.test;

import dev.scx.format.FormatToNodeException;
import dev.scx.format.NodeToFormatException;
import dev.scx.format.xml.XmlNodeConverter;
import dev.scx.format.xml.XmlNodeConverterOptions;
import org.testng.annotations.Test;

public class XmlFormatTest {

    private final static String xml = """
        <root>
            <user>
                一些额外文本
                <id>12345</id>
                <name>小明</name>
                <nickname>明哥&#128640;</nickname>
                <active>true</active>
                <score>99.99</score>
                <address>
                    <city>北京</city>
                    <zipcode>100000</zipcode>
                    <coordinates>
                        <lat>39.9042</lat>
                        <lng>116.4074</lng>
                    </coordinates>
                </address>
                <tags>程序员</tags>
                <tags>摄影师</tags>
                <tags>旅行者</tags>
                <metadata>
                    <created_at>2025-07-09T12:34:56Z</created_at>
                    <updated_at/>
                    <roles>admin</roles>
                    <roles>editor</roles>
                    <roles>
                        <custom>superuser</custom>
                    </roles>
                </metadata>
            </user>
            <posts>
                <id>post-001</id>
                <title>第一篇文章</title>
                <content>这是第一篇文章的内容，包含一些 &lt;b&gt;HTML&lt;/b&gt; 标签。</content>
                <comments>
                    <user>小红</user>
                    <message>写得很好！</message>
                </comments>
                <comments>
                    <user>小刚</user>
                    <message>赞&#128077;</message>
                </comments>
            </posts>
            <posts>
                <id>post-002</id>
                <title>第二篇文章</title>
                <content>这是第二篇文章，内容更丰富。</content>
                <comments>
                    <item>1</item>
                    <item>2</item>
                    <item>3</item>
                </comments>
                <comments>
                    <item>4</item>
                    <item>5</item>
                    <item>6</item>
                </comments>
            </posts>
            <config>
                <theme>dark</theme>
                <notifications>
                    <email>true</email>
                    <sms>false</sms>
                    <push>true</push>
                </notifications>
                <experimental>true</experimental>
                <experimental>false</experimental>
                <experimental/>
                <experimental>beta</experimental>
            </config>
            <misc>123</misc>
            <misc>字符串</misc>
            <misc/>
            <misc>
                <nested>
                    <array>1</array>
                    <array>2</array>
                    <array>3</array>
                    <array>
                        <deep>value</deep>
                    </array>
                </nested>
            </misc>
        </root>
        """;

    private static final XmlNodeConverter xmlNodeConverter = new XmlNodeConverter();

    public static void main(String[] args) throws FormatToNodeException, NodeToFormatException {
        test1();
    }

    @Test
    public static void test1() throws FormatToNodeException, NodeToFormatException {

        var node = xmlNodeConverter.formatToNode(xml, new XmlNodeConverterOptions());

        var xml1 = xmlNodeConverter.nodeToFormatString(node, new XmlNodeConverterOptions());

    }

}
