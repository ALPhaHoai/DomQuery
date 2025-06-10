package org.domquery

import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.*

class NodeSiblingsTest {
    @Test
    fun testSiblingsFunction() {
        val xml = """
        <hierarchy>
            <node text="A" class="alpha"/>
            <node text="B" class="beta"/>
            <node text="C" class="alpha"/>
        </hierarchy>
    """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        val nodeB = root.querySelector("""[text="B"]""")

        // Null node returns empty list
        val nullNode: Node? = null
        assertTrue(nullNode.siblings().isEmpty())

        // Node B should have two siblings: node A and node C
        val siblingsOfB = nodeB.siblings()
        assertEquals(2, siblingsOfB.size)
        val siblingTexts = siblingsOfB.map { (it as Element).getAttribute("text") }
        assertTrue(siblingTexts.containsAll(listOf("A", "C")))
        assertFalse(siblingTexts.contains("B"))

        // With selector: only sibling with class="alpha"
        val selector = """[class="alpha"]"""
        val alphaSiblings = nodeB.siblings(selector)
        assertEquals(2, alphaSiblings.size)
        val alphaTexts = alphaSiblings.map { (it as Element).getAttribute("text") }
        assertEquals(setOf("A", "C"), alphaTexts.toSet())

        // Sibling where there are no siblings
        val singleXml = """<hierarchy><node text="lonely" /></hierarchy>"""
        val singleRoot = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(singleXml.byteInputStream()).documentElement
        val lonely = singleRoot.querySelector("""[text="lonely"]""")
        assertTrue(lonely.siblings().isEmpty())
    }
}