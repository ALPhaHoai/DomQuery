package org.domquery

import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NodePrevTest {

    private fun load(xml: String): Element =
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement

    @Test
    fun prev_returns_immediate_previous_element_sibling_or_null_if_first_child() {
        val xml = """
            <root>
                <node id="1"/>
                <node id="2"/>
                <node id="3"/>
            </root>
        """.trimIndent()
        val root = load(xml)
        val n3 = root.querySelector("""[id="3"]""")
        val n2 = n3.prev()
        assertNotNull(n2)
        assertEquals("2", (n2 as Element).getAttribute("id"))
        val n1 = n2.prev()
        assertNotNull(n1)
        assertEquals("1", (n1 as Element).getAttribute("id"))
        // n1 has no previous element sibling
        assertNull(n1.prev())
    }

    @Test
    fun prev_with_selector_returns_previous_matching_element_sibling_or_null_if_no_match() {
        val xml = """
            <root>
                <node id="a"/>
                <node class="foo" id="b"/>
                <node id="c"/>
            </root>
        """.trimIndent()
        val root = load(xml)
        val n3 = root.querySelector("""[id="c"]""")
        // previous node is class="foo"
        val n2 = n3.prev(".foo")
        assertNotNull(n2)
        assertEquals("b", (n2 as Element).getAttribute("id"))
    }

    @Test
    fun prev_with_selector_returns_null_when_no_matching_previous_element_sibling_exists() {
        val xml = """
            <root>
                <node id="a"/>
                <node class="foo"/>
                <node id="b"/>
            </root>
        """.trimIndent()
        val root = load(xml)
        val n3 = root.querySelector("""[id="b"]""")
        // immediate previous is class="foo", but it doesn't match selector "bar"
        assertNull(n3.prev(".bar"))
        // immediate previous with selector "foo"
        assertNotNull(n3.prev(".foo"))
    }

    @Test
    fun prev_skips_non_element_siblings_and_returns_previous_element_sibling() {
        val xml = """
            <root>
                <node id="1"/><!-- comment -->
                Text
                <node id="2"/>
            </root>
        """.trimIndent()
        val root = load(xml)
        val n2 = root.querySelector("""[id="2"]""")
        val n1 = n2.prev()
        assertNotNull(n1)
        assertEquals("1", (n1 as Element).getAttribute("id"))
    }

    @Test
    fun prev_returns_null_when_called_on_the_first_element_child_of_the_parent() {
        val xml = """
            <root>
                <node id="first"/>
                <node id="second"/>
            </root>
        """.trimIndent()
        val root = load(xml)
        val n1 = root.querySelector("""[id="first"]""")
        assertNull(n1.prev())
    }

    @Test
    fun prev_ignores_non_element_types_and_returns_previous_element_for_cdata_and_comment_siblings() {
        // Add CDATA and comment nodes as siblings
        val xml = """
            <root>
                <node id="a"/>
                <!-- comment node -->
                <![CDATA[some text]]>
                <node id="b"/>
            </root>
        """.trimIndent()
        val root = load(xml)
        val n2 = root.querySelector("""[id="b"]""")
        val n1 = n2.prev()
        assertNotNull(n1)
        assertEquals("a", (n1 as Element).getAttribute("id"))
    }

    @Test
    fun prev_returns_null_when_called_on_null_node_reference() {
        assertNull((null as org.w3c.dom.Node?).prev())
    }
}