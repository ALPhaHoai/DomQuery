package org.domquery

import org.junit.jupiter.api.*
import org.w3c.dom.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeQuerySelectorAllTest {

    @Test
    fun query_selector_all_returns_all_children_matching_complex_selector() {
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
    fun query_selector_all_with_comma_separated_selectors_returns_union_of_matches() {
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
    fun query_selector_all_returns_distinct_results_for_repeated_selector() {
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
    fun query_selector_all_finds_elements_by_class_name_only() {
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