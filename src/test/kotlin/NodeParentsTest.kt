package org.domquery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

class NodeParentsTest {
    private fun parse(xml: String): Element =
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.trimIndent().byteInputStream()).documentElement

    private fun Element.firstById(id: String): Node? =
        this.querySelector("[id=\"$id\"]")

    @Test
    fun parents_returns_all_ancestor_elements_in_closer_to_further_order() {
        val xml = """
        <hierarchy id ="hierarchy">
            <node id="grandparent">
                <node id="parent">
                    <node id="child"/>
                </node>
            </node>
        </hierarchy>
    """.trimIndent()
        val root = parse(xml)
        val child = root.querySelector("[id=\"child\"]")!!
        val ancestors = child.parents()
        // Get id for nodes, or nodeName if no id
        val ids = ancestors.map { (it as? Element)?.getAttribute("id") ?: it.nodeName }
        assertEquals(listOf("parent", "grandparent", "hierarchy"), ids)
    }

    @Test
    fun parents_selector_returns_only_matching_ancestors() {
        val xml = """
            <hierarchy>
                <A class="c1">
                    <B class="c1" id="p1">
                        <C class="c2" id="child1"/>
                        <C class="c1 c2" id="child2"/>
                    </B>
                </A>
            </hierarchy>
        """
        val root = parse(xml)

        val c1 = root.firstById("child1")!!
        val tagMatch = c1.parents("B")
        assertEquals(1, tagMatch.size)
        assertEquals("B", tagMatch[0].nodeName)

        val byClass = c1.parents("[class~=\"c1\"]")
        assertEquals(2, byClass.size)
        val classNames = byClass.map { (it as Element).getAttribute("class") }
        assertEquals(listOf("c1", "c1"), classNames.map { it.split(" ")[0] })

        val byId = c1.parents("#p1")
        assertEquals(1, byId.size)
        assertEquals("p1", (byId[0] as Element).getAttribute("id"))

        val none = c1.parents(".doesnotexist")
        assertTrue(none.isEmpty())
    }

    @Test
    fun parents_does_not_include_self() {
        val xml = """
            <hierarchy>
                <node id="n"/>
            </hierarchy>
        """
        val root = parse(xml)
        val n = root.firstById("n")!!
        val ps = n.parents()
        assertFalse(ps.contains(n))
    }

    @Test
    fun root_element_has_no_parents() {
        val xml = "<Root/>"
        val root = parse(xml)
        assertTrue(root.parents().isEmpty())
    }

    @Test
    fun parents_handles_null_and_returns_empty_list() {
        val node: Node? = null
        assertEquals(0, node.parents().size)
        assertEquals(0, node.parents("A").size)
    }

    @Test
    fun parents_matches_case_sensitive_tags_and_attributes() {
        val xml = """
            <HIERARCHY>
                <Parent>
                    <child/>
                </Parent>
                <parent>
                    <child/>
                </parent>
            </HIERARCHY>
        """
        val root = parse(xml)
        val childUpper = root.querySelector("Parent > child")
        val upAncestors = childUpper?.parents("Parent")
        assertEquals(1, upAncestors?.size)
        assertEquals("Parent", (upAncestors?.get(0) as Element).tagName)

        val childLower = root.querySelector("parent > child")
        val loAncestors = childLower?.parents("parent")
        assertEquals(1, loAncestors?.size)
        assertEquals("parent", (loAncestors?.get(0) as Element).tagName)
    }

    @Test
    fun parents_with_attribute_selectors() {
        val xml = """
            <hierarchy>
                <group type="foo">
                    <box enabled="true">
                        <node/>
                    </box>
                </group>
            </hierarchy>
        """
        val root = parse(xml)
        val node = root.querySelector("node")!!
        assertEquals(1, node.parents("[enabled]").size)
        assertEquals(1, node.parents("[type=\"foo\"]").size)
        assertTrue(node.parents("[type=\"bar\"]").isEmpty())
    }

    @Test
    fun parents_returns_all_ancestors_up_to_root() {
        val xml = """
            <h>
                <x>
                    <y>
                        <z>
                            <k id="kid"/>
                        </z>
                    </y>
                </x>
            </h>
        """
        val root = parse(xml)
        val kid = root.querySelector("#kid")!!
        val tags = kid.parents().map { it.nodeName }
        assertEquals(listOf("z", "y", "x", "h"), tags)
    }
}