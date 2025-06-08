package org.domquery

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.awt.Point
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory


// Place near the top of your file
private object SelectorRegex {
    val Id = Regex("""#([A-Za-z0-9_\-:]+)""")
    val Class = Regex("""\.([A-Za-z_\-][A-Za-z0-9_\-]*)""")
    val Attribute = Regex("""\[\s*([A-Za-z0-9_\-:]+)\s*=\s*(?:(['"])(.*?)\2|([^\]\s]+))\s*]""")
    val Tag = Regex("""^[A-Za-z0-9_\-]+""")
}

private object BoundsRegex {
    // For parsing Android-style bounds: "[left,top][right,bottom]"
    val Bounds = Regex("""\[(\d+),(\d+)]\[(\d+),(\d+)]""")
}

val splitCommaNotInBracket = Regex(""",(?![^\[]*\])""")
val spaceOutsideAttrBracketsRegex = Regex("""\s+(?=(?:[^"]*"[^"]*")*[^"]*$)(?=(?:[^\[]*\[[^\]]*\])*[^\[]*$)""")

private const val NODE_TAG = "node"

sealed class Combinator {
    object Descendant : Combinator();
    object Child : Combinator();
    object Adjacent : Combinator();
    object Sibling : Combinator()
}

data class SelectorStep(val selector: SimpleSelector, val combinator: Combinator)

data class SimpleSelector(
    val tagName: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val id: String? = null,
    val classNames: Set<String> = emptySet()
)

fun parseSelectorList(selector: String): List<SimpleSelector> {
    return selector.trim().split(spaceOutsideAttrBracketsRegex).map { parseSimpleSelector(it) }
}

/** Parses a simplified CSS selector into its components. */
fun parseSimpleSelector(selector: String): SimpleSelector {
    val selectorTrim = selector.trim()
    val selectorWithoutAttrs = selectorTrim.replace(Regex("""\[[^\]]*]"""), "")
    var tagName: String? = null
    var id: String? = null
    val attributes = mutableMapOf<String, String>()

    // Tag at start
    SelectorRegex.Tag.find(selectorTrim)?.let { tagName = it.value }

    // ID with # notation
    SelectorRegex.Id.find(selectorTrim)?.let { id = it.groupValues[1] }

    val classNames = SelectorRegex.Class.findAll(selectorWithoutAttrs).map { it.groupValues[1] }.toSet()

    // Attribute selectors [attr="val"]
    SelectorRegex.Attribute.findAll(selectorTrim).forEach { m ->
        val attrName = m.groupValues[1]
        val attrValue = m.groupValues[3].ifEmpty { m.groupValues[4] }
        attributes[attrName] = attrValue
    }

    return SimpleSelector(tagName, attributes, id, classNames)
}

class XmlDocument(private val document: String) {
    private val doc = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(ByteArrayInputStream(document.toByteArray(Charsets.UTF_8)))

    fun querySelectorPoint(cssSelector: String): Point? {
        return doc.documentElement
            .querySelector(cssSelector)
            ?.toCenterPoint()
    }

    fun querySelector(cssSelector: String): Node? {
        return doc.documentElement.querySelector(cssSelector)
    }

    private fun findRootNode(root: Map<String, String>): Element? {
        val cssSelector = mapToCssSelector(root)
        return doc.documentElement.querySelector(cssSelector) as? Element
    }

    private fun <T> findGenericDescendant(
        node: Node,
        attributes: Map<String, String>,
        skipSelf: Boolean = false,
        resultSelector: (Element) -> T?
    ): T? {
        if (!skipSelf && node.nodeType == Node.ELEMENT_NODE && node.nodeName == "node") {
            val element = node as Element
            val isMatch = attributes.all { (key, value) -> element.getAttribute(key) == value }
            if (isMatch) return resultSelector(element)
        }
        val childNodes = node.childNodes
        for (i in 0 until childNodes.length) {
            val child = childNodes.item(i)
            if (child.nodeType == Node.ELEMENT_NODE && child.nodeName == "node") {
                findGenericDescendant(child, attributes, false, resultSelector)?.let { return it }
            }
        }
        return null
    }

    private fun mapToCssSelector(attrs: Map<String, String>): String =
        attrs.entries.joinToString("") { """[${it.key}="${it.value}"]""" }
}


// Parser for a single selector group, e.g. 'A > B + C'
fun parseSelectorChain(selector: String): List<SelectorStep> {
    val re = Regex("""\s*([>+~])\s*""")
    val tokens = re.split(selector)
    val combinators = re.findAll(selector).map {
        when (it.groupValues[1]) {
            ">" -> Combinator.Child
            "+" -> Combinator.Adjacent
            "~" -> Combinator.Sibling
            else -> Combinator.Descendant
        }
    }.toList()
    val out = mutableListOf<SelectorStep>()
    out.add(SelectorStep(parseSimpleSelector(tokens[0]), Combinator.Descendant))
    for (i in 1 until tokens.size) {
        out.add(SelectorStep(parseSimpleSelector(tokens[i]), combinators[i - 1]))
    }
    return out
}

fun previousElementSibling(node: Node): Node? {
    var sib = node.previousSibling
    while (sib != null) {
        if (sib.nodeType == Node.ELEMENT_NODE) return sib
        sib = sib.previousSibling
    }
    return null
}

fun matchesStep(node: Node?, step: SelectorStep): Node? {
    if (node == null || node.nodeType != Node.ELEMENT_NODE) return null
    val elem = node as Element
    return if (elem.matches(step.selector)) node else null
}

fun matchSelectorChain(node: Node, steps: List<SelectorStep>): Boolean {
    fun inner(idx: Int, base: Node): Boolean {
        if (idx < 0) return true
        val step = steps[idx]
        if (idx == steps.lastIndex) {
            return matchesStep(base, step) != null && inner(idx - 1, base)
        }

        return when (step.combinator) {
            is Combinator.Descendant -> {
                var current = base.parentNode
                while (current != null) {
                    if (matchesStep(current, steps[idx]) != null && inner(idx - 1, current)) return true
                    current = current.parentNode
                }
                false
            }

            is Combinator.Child -> {
                val parent = base.parentNode
                parent != null && matchesStep(parent, steps[idx]) != null && inner(idx - 1, parent)
            }

            is Combinator.Adjacent -> {
                val prev = previousElementSibling(base)
                prev != null && matchesStep(prev, steps[idx]) != null && inner(idx - 1, prev)
            }

            is Combinator.Sibling -> {
                var prev = previousElementSibling(base)
                while (prev != null) {
                    if (matchesStep(prev, steps[idx]) != null && inner(idx - 1, prev)) return true
                    prev = previousElementSibling(prev)
                }
                false
            }
        }
    }
    return inner(steps.lastIndex, node)
}


fun Node.toCenterPoint(): Point? {
    if (this.nodeType != Node.ELEMENT_NODE) return null
    val element = this as Element
    val boundsStr = element.getAttribute("bounds")
    val match = BoundsRegex.Bounds.matchEntire(boundsStr) ?: return null
    val (left, top, right, bottom) = match.destructured
    val cx = (left.toInt() + right.toInt()) / 2
    val cy = (top.toInt() + bottom.toInt()) / 2
    return Point(cx, cy)
}

fun Node.querySelector(selector: String): Node? {
    if (selector.isBlank()) return null
    val selectorGroups = selector.split(splitCommaNotInBracket).map { it.trim() }
    val selectorChains = selectorGroups.map(::parseSelectorChain)
    val stack = ArrayDeque<Node>()
    stack.add(this)
    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        if (node.nodeType == Node.ELEMENT_NODE) {
            for (chain in selectorChains) {
                if (matchSelectorChain(node, chain)) {
                    return node
                }
            }
        }
        val children = node.childNodes
        for (i in 0 until children.length) {
            stack.add(children.item(i))
        }
    }
    return null
}


fun Node.querySelectorAll(selector: String): List<Node> {
    val result = mutableListOf<Node>()
    val selectorGroups = selector.split(splitCommaNotInBracket).map { it.trim() }
    val selectorChains = selectorGroups.map(::parseSelectorChain)
    val stack = ArrayDeque<Node>()
    stack.add(this)
    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        if (node.nodeType == Node.ELEMENT_NODE) {
            for (chain in selectorChains) {
                if (matchSelectorChain(node, chain)) {
                    result.add(node)
                    break
                }
            }
        }
        // Add children to stack
        val children = node.childNodes
        for (i in 0 until children.length) {
            stack.add(children.item(i))
        }
    }
    return result.distinct()
}


fun Element.matches(selector: SimpleSelector): Boolean {
    selector.tagName?.let { if (this.tagName != it) return false }
    selector.id?.let {
        val idValue = this.getAttribute("resource-id").takeIf { s -> s.isNotEmpty() }
            ?: this.getAttribute("id")
        if (idValue != it) return false
    }
    if (selector.classNames.isNotEmpty()) {
        val attrClasses = this.getAttribute("class")
            .orEmpty()
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .toSet()
        if (!selector.classNames.all { it in attrClasses }) return false
    }
    for ((key, value) in selector.attributes) {
        if (this.getAttribute(key) != value) return false
    }
    return true
}