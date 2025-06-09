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

    @Test
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
    }

    @Test
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
    }

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

    @Test
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
    }

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

    @Test
    fun testClassTildeSelector() {
        val xml = """
            <hierarchy>
              <node class="foo button1 bar" text="ok"/>
              <node class="foo bar" text="fail"/>
            </hierarchy>
        """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement

        val node = root.querySelector("""[class~="button1"]""")
        assertNotNull(node)
        assertEquals("ok", (node as Element).getAttribute("text"))

        // Should not match node with class="foo bar"
        val none = root.querySelector("""[class~="notfound"]""")
        assertNull(none)
    }

    @Test
    fun testClassCaretSelector() {
        val xml = """
            <hierarchy>
              <node class="button1 foo bar" text="ok"/>
              <node class="foo button1 bar" text="fail"/>
            </hierarchy>
        """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement

        val node = root.querySelector("""[class^="button1"]""")
        assertNotNull(node)
        assertEquals("ok", (node as Element).getAttribute("text"))
    }

    @Test
    fun testClassSubstringSelector() {
        val xml = """
            <hierarchy>
              <node class="foo_button1_bar" text="ok"/>
              <node class="foo bar" text="fail"/>
            </hierarchy>
        """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement

        val node = root.querySelector("""[class*="button1"]""")
        assertNotNull(node)
        assertEquals("ok", (node as Element).getAttribute("text"))
    }

    @Test
    fun testClassDollarSelector() {
        val xml = """
            <hierarchy>
              <node class="foo bar button1" text="ok"/>
              <node class="foo button1 bar" text="fail"/>
            </hierarchy>
        """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement

        val node = root.querySelector("""[class$="button1"]""")
        assertNotNull(node)
        assertEquals("ok", (node as Element).getAttribute("text"))
    }

    @Test
    fun testClassDashSelector() {
        val xml = """
            <hierarchy>
              <node class="button1-foo" text="dash"/>
              <node class="button1" text="exact"/>
              <node class="button11" text="fail"/>
            </hierarchy>
        """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement

        val node1 = root.querySelector("""[class|="button1"]""")
        assertNotNull(node1)
        val cls = (node1 as Element).getAttribute("class")
        assertTrue(cls == "button1" || cls.startsWith("button1-"))
    }

    @Test
    fun testAttributePresenceOnly() {
        val xml = """<hierarchy><node foo="bar"/></hierarchy>"""
        val root =
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.byteInputStream()).documentElement
        assertNotNull(root.querySelector("[foo]"))
        assertNull(root.querySelector("[baz]"))
    }

    @Test
    fun testAttributeWithSingleQuotes() {
        val xml = """
        <hierarchy>
            <node class='single_quote_test' text='Hello single quote'/>
            <node class="double_quote_test" text="Hello double quote"/>
        </hierarchy>
    """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement

        // Test single quotes in selector
        assertNotNull(root.querySelector("[text='Hello single quote']"))
        assertNotNull(root.querySelector("[class='single_quote_test']"))

        // Also confirm matching with double quoted values for completeness
        assertNotNull(root.querySelector("[text=\"Hello double quote\"]"))
        assertNotNull(root.querySelector("[class=\"double_quote_test\"]"))
    }

    @Test
    fun testTagNameCaseSensitivity() {
        val xml = """<hierarchy><Node text="yes"/><node text="no"/></hierarchy>"""
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement

        // Should only match the lowercase 'node'
        val matchLower = root.querySelector("node")
        assertNotNull(matchLower)
        assertEquals("no", (matchLower as Element).getAttribute("text"))

        // Should only match the uppercase 'Node'
        val matchUpper = root.querySelector("Node")
        assertNotNull(matchUpper)
        assertEquals("yes", (matchUpper as Element).getAttribute("text"))
    }

    @Test
    fun testAttributeNameCaseSensitivity() {
        val xml = """<hierarchy>
        <node foo="correct" FOO="wrong" FoO="alsoWrong"/>
    </hierarchy>"""
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement

        // These should each match only the corresponding case
        val matchLower = root.querySelector("""[foo="correct"]""")
        assertNotNull(matchLower)

        val matchUpper = root.querySelector("""[FOO="wrong"]""")
        assertNotNull(matchUpper)
        assertEquals("wrong", (matchUpper as Element).getAttribute("FOO"))

        val matchMixed = root.querySelector("""[FoO="alsoWrong"]""")
        assertNotNull(matchMixed)
        assertEquals("alsoWrong", (matchMixed as Element).getAttribute("FoO"))

        // Should not match with wrong case
        assertNull(root.querySelector("""[Foo="anything"]"""))
    }

    @Test
    fun testAttributeValueCaseSensitivity() {
        val xml = """<hierarchy>
        <node foo="Bar"/>
    </hierarchy>"""
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement

        // Attribute value matching should be case-sensitive (your selector code uses simple ==)
        assertNotNull(root.querySelector("""[foo="Bar"]"""))
        assertNull(root.querySelector("""[foo="bar"]""")) // Should not match if case-sensitive
    }

    @Test
    fun testAttributeWithSpecialCharacters() {
        // "label" uses a comma; "name" uses brackets and spaces
        val xml = """
        <hierarchy>
            <node label="foo,bar"/>
            <node name="foo[bar]"/>
            <node label="plain"/>
        </hierarchy>
    """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement

        // Should find the node with label containing a comma
        val byComma = root.querySelector("""[label="foo,bar"]""")
        assertNotNull(byComma)
        assertEquals("foo,bar", (byComma as Element).getAttribute("label"))

        // Should find the node with name containing brackets
        val byBracket = root.querySelector("""[name="foo[bar]"]""")
        assertNotNull(byBracket)
        assertEquals("foo[bar]", (byBracket as Element).getAttribute("name"))

        // Should NOT match a node with just label="plain"
        assertNull(root.querySelector("""[label="foo"]"""))
    }

    @Test
    fun testFlatVsNestedStructure() {
        val xml = """
        <hierarchy>
            <node class="flat" text="f1"/>
            <node class="flat" text="f2"/>
            <node class="parent" text="parent1">
                <node class="nested" text="n1"/>
                <node class="nested" text="n2"/>
            </node>
            <node class="parent" text="parent2">
                <node class="nested" text="n3"/>
            </node>
        </hierarchy>
    """.trimIndent()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement

        // Flat nodes: direct children of <hierarchy>
        val flats = root.querySelectorAll("""[class="flat"]""")
        assertEquals(2, flats.size)
        val flatTexts = flats.map { (it as Element).getAttribute("text") }.toSet()
        assertEquals(setOf("f1", "f2"), flatTexts)

        // Nested nodes: direct children of <node class="parent">
        val nesteds = root.querySelectorAll("""[class="parent"] > [class="nested"]""")
        assertEquals(3, nesteds.size)
        val nestedTexts = nesteds.map { (it as Element).getAttribute("text") }.toSet()
        assertEquals(setOf("n1", "n2", "n3"), nestedTexts)

        // All descendants with class 'nested'
        val allNestedDescendants = root.querySelectorAll("""[class="nested"]""")
        assertEquals(3, allNestedDescendants.size) // Shouldn't match flat nodes

        // Should not return 'nested' nodes for flat selector
        val wronglyNested = root.querySelectorAll("""[class="flat"] > [class="nested"]""")
        assertTrue(wronglyNested.isEmpty())

        // Descendant combinator: parent and all nested below, even deeper
        val asDescendant = root.querySelectorAll("""[class="parent"] [class="nested"]""")
        assertEquals(3, asDescendant.size)

        // Adjacent combinator: n1 and n2 are siblings, n3 is alone
        val adjacent = root.querySelectorAll(
            """[class="parent"] > [class="nested"][text="n1"] + [class="nested"][text="n2"]"""
        )
        assertEquals(1, adjacent.size)
        val adjText = (adjacent[0] as Element).getAttribute("text")
        assertEquals("n2", adjText)
    }
}