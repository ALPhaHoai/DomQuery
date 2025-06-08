package org.domquery

import org.junit.jupiter.api.*
import org.w3c.dom.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeQuerySelectorExtraTest {

    @Test
    fun testByIndexAttribute() {
        val xml = """<hierarchy><node index="2"/></hierarchy>"""
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        val node = root.querySelector("""[index="2"]""")
        assertNotNull(node)
        val e = node as Element
        assertEquals("2", e.getAttribute("index"))
    }

    @Test
    fun testByTagNameNode() {
        val xml = """<hierarchy><node index="99"/></hierarchy>"""
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        // Should find the node element
        val found = root.querySelector("node")
        assertNotNull(found)
        val e = found as Element
        assertEquals("99", e.getAttribute("index"))
    }

    @Test
    fun testByTagNameNoMatch() {
        val xml = """<hierarchy><node/></hierarchy>"""
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        // No <foobar> should exist
        assertNull(root.querySelector("foobar"))
        assertTrue(root.querySelectorAll("foobar").isEmpty())
    }

    @Test
    fun testMalformedSelector() {
        val xml = """<hierarchy><node/></hierarchy>"""
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        // Empty or blank:
        assertNull(root.querySelector(""))
        assertNull(root.querySelector("    "))
    }

    @Test
    fun testDescendantCombinator() {
        val xml = """
        <hierarchy>
            <node class="android_widget_FrameLayout">
                <node>
                    <node class="android_widget_Button" text="btn1"/>
                </node>
            </node>
        </hierarchy>
        """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        val node = root.querySelector("""[class="android_widget_FrameLayout"] [class="android_widget_Button"]""")
        assertNotNull(node)
        assertEquals("android_widget_Button", (node as Element).getAttribute("class"))
        assertEquals("btn1", node.getAttribute("text"))
    }

    @Test
    fun testChildCombinatorWithSpaces() {
        val xml = """
        <hierarchy>
            <node class="android_widget_FrameLayout">
                <node/>
            </node>
        </hierarchy>
        """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        val node = root.querySelector("""[class="android_widget_FrameLayout"] > node""")
        assertNotNull(node)
        val parent = node.parentNode as? Element
        assertEquals("android_widget_FrameLayout", parent!!.getAttribute("class"))
    }

   /* @Test
    fun testAdjacentSiblingCombinator() {
        val xml = """
        <hierarchy>
          <node>
            <node class="android_view_View" text="A"/>
            <node class="android_widget_Button" text="B"/>
          </node>
        </hierarchy>
        """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        val node = root.querySelector(
            """[class="android_view_View"][text="A"] + [class="android_widget_Button"][text="B"]"""
        )
        assertNotNull(node)
        assertEquals("B", (node as Element).getAttribute("text"))
    }*/

    /*@Test
    fun testGeneralSiblingCombinator() {
        val xml = """
        <hierarchy>
          <node>
            <node class="android_widget_TextView" text="A"/>
            <node class="android_widget_TextView" text="B"/>
            <node class="android_widget_TextView" text="C"/>
          </node>
        </hierarchy>
        """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        val node =
            root.querySelector("""[class="android_widget_TextView"][text="A"] ~ [class="android_widget_TextView"][text="C"]""")
        assertNotNull(node)
        assertEquals("C", (node as Element).getAttribute("text"))
    }*/

    @Test
    fun testAllChildrenOfParent() {
        val xml = """
        <hierarchy>
          <node resource-id="container1">
            <node class="android_widget_Button" text="btn1"/>
            <node class="android_widget_Button" text="btn2"/>
          </node>
        </hierarchy>
        """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        // All buttons child of container1
        val nodes = root.querySelectorAll("""[resource-id="container1"] > [class="android_widget_Button"]""")
        assertEquals(2, nodes.size)
        val texts = nodes.map { (it as Element).getAttribute("text") }.toSet()
        assertEquals(setOf("btn1", "btn2"), texts)
    }

    @Test
    fun testMultipleCommaSelectorReturnsAll() {
        val xml = """
        <hierarchy>
            <node class="android_widget_Button"/>
            <node class="android_widget_Button"/>
            <node class="android_widget_ListView"/>
        </hierarchy>
        """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        val res = root.querySelectorAll("""[class="android_widget_Button"], [class="android_widget_ListView"]""")
        // Should find BOTH buttons AND the listview node
        assertEquals(3, res.size)
        val classes = res.map { (it as Element).getAttribute("class") }.toSet()
        assertTrue("android_widget_Button" in classes)
        assertTrue("android_widget_ListView" in classes)
    }

    @Test
    fun testAttributeWithSpaces() {
        // Add a node with attribute containing spaces and match it
        val xml = """<hierarchy>
          <node class="android_widget_TextView" text="Hello world"/>
          <node class="android_widget_TextView" text="Hello, world"/>
        </hierarchy>"""
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        assertNotNull(root.querySelector("""[text="Hello world"]"""))
        assertNotNull(root.querySelector("""[text="Hello, world"]"""))
    }

   /* @Test
    fun testChainedAttributeAndClass() {
        val xml = """
        <hierarchy>
            <node class="button1 button2" text="btn1"/>
            <node class="button2 button3" text="btn2"/>
        </hierarchy>
        """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        // Use [class~="xxx"] for multivalue class
        val btn1 = root.querySelector("""[class~="button1"][class~="button2"][text="btn1"]""")
        assertNotNull(btn1)
        assertEquals("btn1", (btn1 as Element).getAttribute("text"))
    }*/

    @Test
    fun testDistinctResults() {
        val xml = """
        <hierarchy>
            <node class="android_widget_Button"/>
            <node class="android_widget_Button"/>
        </hierarchy>
        """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        // Should not include the same node more than once
        val allButtonsTwice =
            root.querySelectorAll("""[class="android_widget_Button"], [class="android_widget_Button"]""")
        assertEquals(2, allButtonsTwice.size)
    }

    @Test
    fun testFindByIdResourceId() {
        val xml = """
        <hierarchy>
            <node resource-id="logo"/>
        </hierarchy>
        """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        // node with resource-id="logo"
        val node = root.querySelector("#logo")
        assertNotNull(node)
        assertEquals("logo", (node as Element).getAttribute("resource-id"))
    }

    @Test
    fun testFindByNonexistentId() {
        val xml = """<hierarchy><node resource-id="foo"/></hierarchy>"""
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        assertNull(root.querySelector("#xyz123abc"))
    }

    @Test
    fun testFindByClassOnly() {
        // There are three nodes with class="android_widget_TextView"
        val xml = """
        <hierarchy>
            <node class="android_widget_TextView" text="A"/>
            <node class="android_widget_TextView" text="B"/>
            <node class="android_widget_TextView" text="C"/>
        </hierarchy>
        """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement
        val nodes = root.querySelectorAll("""[class="android_widget_TextView"]""")
        assertEquals(3, nodes.size) // A, B, C
        val texts = nodes.map { (it as Element).getAttribute("text") }
        assertTrue("A" in texts && "B" in texts && "C" in texts)
    }
}