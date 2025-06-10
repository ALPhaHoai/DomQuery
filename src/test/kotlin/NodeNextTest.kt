package org.domquery

import org.junit.jupiter.api.Test
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NodeNextTest {
    @Test
    fun next_returns_immediate_element_sibling_or_null_if_last() {
        val xml = """
        <hierarchy>
            <node text="A" class="alpha"/>
            <node text="B" class="beta"/>
            <node text="C" class="alpha"/>
        </hierarchy>
    """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        val nodeA = root.querySelector("""[text="A"]""")
        val nodeB = root.querySelector("""[text="B"]""")
        val nodeC = root.querySelector("""[text="C"]""")

        // Next after nodeA should be nodeB
        assertEquals(nodeB, nodeA.next())
        // Next after nodeB should be nodeC
        assertEquals(nodeC, nodeB.next())
        // Next after nodeC is null (it's last)
        assertNull(nodeC.next())
    }

    @Test
    fun next_with_selector_returns_only_matching_next_element_sibling() {
        val xml = """
        <hierarchy>
            <node text="A" class="alpha"/>
            <node text="B" class="beta"/>
            <node text="C" class="alpha"/>
        </hierarchy>
    """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        val nodeA = root.querySelector("""[text="A"]""")
        val nodeB = root.querySelector("""[text="B"]""")
        val nodeC = root.querySelector("""[text="C"]""")

        val selectorAlpha = """[class="alpha"]"""
        val selectorBeta = """[class="beta"]"""
        val selectorNone = """[class="unknown"]"""

        // nodeA.next(selectorAlpha) -- immediate sibling is class=beta, so returns null
        assertNull(nodeA.next(selectorAlpha))
        // nodeB.next(selectorAlpha) -- immediate sibling is nodeC (class=alpha), matches
        assertEquals(nodeC, nodeB.next(selectorAlpha))
        // nodeA.next(selectorBeta) -- immediate sibling is nodeB, matches
        assertEquals(nodeB, nodeA.next(selectorBeta))
        // nodeC.next(selectorAlpha) -- nothing after C
        assertNull(nodeC.next(selectorAlpha))
        // nodeB.next(selectorNone) -- next is nodeC but does not match
        assertNull(nodeB.next(selectorNone))
    }

    @Test
    fun next_returns_null_for_null_node_or_node_with_no_next_element_sibling() {
        val xml = """
        <hierarchy>
            <node text="lonely" />
        </hierarchy>
    """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        val onlyNode = root.querySelector("""[text="lonely"]""")
        assertNull(onlyNode.next())
        // null receiver
        val nullNode: Node? = null
        assertNull(nullNode.next())
    }

    @Test
    fun next_skips_non_element_siblings_and_returns_next_element_sibling() {
        val xml = """
    <hierarchy>
        <node text="A"/>
        <!-- a comment -->
        Some Text
        <node text="B"/>
    </hierarchy>
    """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        val nodeA = root.querySelector("""[text="A"]""")
        val nodeB = root.querySelector("""[text="B"]""")
        assertEquals(nodeB, nodeA.next())
    }
}