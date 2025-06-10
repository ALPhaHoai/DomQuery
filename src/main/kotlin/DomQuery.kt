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
    val Attribute = Regex("""\[\s*([A-Za-z0-9_\-:]+)(?:\s*([~|^$*]?=)\s*(["']?)(.*?)\3)?\s*]""")
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
    object Descendant : Combinator() {
        override fun toString() = " "
    }

    object Child : Combinator() {
        override fun toString() = ">"
    }

    object Adjacent : Combinator() {
        override fun toString() = "+"
    }

    object Sibling : Combinator() {
        override fun toString() = "~"
    }
}

data class SelectorStep(val selector: SimpleSelector, val combinator: Combinator) {
    override fun toString(): String = when (combinator) {
        is Combinator.Descendant -> selector.toString()
        is Combinator.Child -> "> $selector"
        is Combinator.Adjacent -> "+ $selector"
        is Combinator.Sibling -> "~ $selector"
    }
}


enum class AttrOperator {
    EQUALS, DASHMATCH, INCLUDES, PREFIX, SUFFIX, SUBSTRING, PRESENT;

    override fun toString(): String = when (this) {
        EQUALS -> "="
        INCLUDES -> "~="
        DASHMATCH -> "|="
        PREFIX -> "^="
        SUFFIX -> "$="
        SUBSTRING -> "*="
        PRESENT -> ""
    }
}

data class AttributeSelector(
    val attr: String,
    val op: AttrOperator,
    val value: String
) {
    override fun toString(): String = """[$attr${op}"$value"]"""
}

data class SimpleSelector(
    val tagName: String? = null,
    val attrSelectors: List<AttributeSelector> = emptyList(),
    val id: String? = null,
    val classNames: Set<String> = emptySet()
) {
    override fun toString(): String {
        val builder = StringBuilder()
        tagName?.let { builder.append(it) }
        id?.let { builder.append("#").append(it) }
        classNames.forEach { builder.append(".").append(it) }
        attrSelectors.forEach { builder.append(it.toString()) }
        return builder.toString()
    }
}

fun parseSelectorList(selector: String): List<SimpleSelector> {
    return selector.trim().split(spaceOutsideAttrBracketsRegex).map { parseSimpleSelector(it) }
}

/** Parses a simplified CSS selector into its components. */
fun parseSimpleSelector(selector: String): SimpleSelector {
    val selectorTrim = selector.trim()
    val selectorWithoutAttrs = selectorTrim.replace(Regex("""\[[^\]]*]"""), "")
    var tagName: String? = null
    var id: String? = null

    // Tag at start
    SelectorRegex.Tag.find(selectorTrim)?.let { tagName = it.value }

    // ID with # notation
    SelectorRegex.Id.find(selectorTrim)?.let { id = it.groupValues[1] }

    val classNames = SelectorRegex.Class.findAll(selectorWithoutAttrs).map { it.groupValues[1] }.toSet()

    val attrSelectors = mutableListOf<AttributeSelector>()
    SelectorRegex.Attribute.findAll(selectorTrim).forEach { m ->
        val attrName = m.groupValues[1]
        val opStr = m.groupValues[2]
        val attrValue = m.groupValues[4]
        val op = when (opStr) {
            "=" -> AttrOperator.EQUALS
            "~=" -> AttrOperator.INCLUDES
            "|=" -> AttrOperator.DASHMATCH
            "^=" -> AttrOperator.PREFIX
            "$=" -> AttrOperator.SUFFIX
            "*=" -> AttrOperator.SUBSTRING
            null, "" -> AttrOperator.PRESENT
            else -> AttrOperator.EQUALS
        }
        attrSelectors.add(AttributeSelector(attrName, op, attrValue))
    }
    return SimpleSelector(tagName, attrSelectors, id, classNames)
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

fun splitSelector(selector: String): List<Pair<String, Combinator>> {
    val result = mutableListOf<Pair<String, Combinator>>()
    var inAttr = false
    var inString: Char? = null
    var current = StringBuilder()
    var lastCombinator: Combinator = Combinator.Descendant

    fun flush() {
        if (current.isNotEmpty()) {
            result.add(current.toString().trim() to lastCombinator)
            current = StringBuilder()
        }
    }

    var i = 0
    while (i < selector.length) {
        val c = selector[i]
        when {
            inString != null -> {
                current.append(c)
                if (c == inString) inString = null
            }

            c == '"' || c == '\'' -> {
                current.append(c)
                inString = c
            }

            c == '[' -> {
                inAttr = true
                current.append(c)
            }

            c == ']' -> {
                inAttr = false
                current.append(c)
            }

            !inAttr && (c == '>' || c == '+' || c == '~') -> {
                // Found a combinator outside attribute
                flush()
                lastCombinator = when (c) {
                    '>' -> Combinator.Child
                    '+' -> Combinator.Adjacent
                    '~' -> Combinator.Sibling
                    else -> Combinator.Descendant
                }
                // Skip whitespace after combinator
                i++
                while (i < selector.length && selector[i].isWhitespace()) i++
                i--
            }

            !inAttr && c.isWhitespace() -> {
                // Only count whitespace as descendant combinator if not in attribute
                flush()
                lastCombinator = Combinator.Descendant
                // Skip additional whitespace
                while (i + 1 < selector.length && selector[i + 1].isWhitespace()) i++
            }

            else -> {
                current.append(c)
            }
        }
        i++
    }
    flush()
    return result
}

fun parseSelectorChain(selector: String): List<SelectorStep> =
    splitSelector(selector).map { (part, comb) -> SelectorStep(parseSimpleSelector(part), comb) }

fun previousElementSibling(node: Node): Node? {
    var sib = node.previousSibling
    while (sib != null) {
        if (sib.nodeType == Node.ELEMENT_NODE) return sib
        sib = sib.previousSibling
    }
    return null
}

fun Node?.matchesStep(step: SelectorStep): Node? {
    if (this == null || this.nodeType != Node.ELEMENT_NODE) return null
    val elem = this as Element
    return if (elem.matches(step.selector)) this else null
}

fun Node.matchSelectorChain(steps: List<SelectorStep>): Boolean {
    fun inner(node: Node, stepIndex: Int): Boolean {
        if (stepIndex < 0) return true
        val step = steps[stepIndex]
        if (node.matchesStep(step) == null) return false
        if (stepIndex == 0) return true
        val prevStep = steps[stepIndex - 1]
        return when (step.combinator) {
            is Combinator.Descendant -> {
                var ancestor = node.parentNode
                while (ancestor != null) {
                    if (ancestor.matchesStep(prevStep) != null && inner(ancestor, stepIndex - 1)) return true
                    ancestor = ancestor.parentNode
                }
                false
            }

            is Combinator.Child -> {
                val parent = node.parentNode
                parent != null && parent.matchesStep(prevStep) != null && inner(parent, stepIndex - 1)
            }

            is Combinator.Adjacent -> {
                val prev = previousElementSibling(node)
                prev != null && prev.matchesStep(prevStep) != null && inner(prev, stepIndex - 1)
            }

            is Combinator.Sibling -> {
                var prev = previousElementSibling(node)
                while (prev != null) {
                    if (prev.matchesStep(prevStep) != null && inner(prev, stepIndex - 1)) return true
                    prev = previousElementSibling(prev)
                }
                false
            }
        }
    }
    return inner(this, steps.lastIndex)
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
                if (node.matchSelectorChain(chain)) {
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
                if (node.matchSelectorChain(chain)) {
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
    for (attrsel in selector.attrSelectors) {
        val actual = this.getAttribute(attrsel.attr).orEmpty()
        when (attrsel.op) {
            AttrOperator.PRESENT -> if (!this.hasAttribute(attrsel.attr)) return false
            AttrOperator.EQUALS -> if (actual != attrsel.value) return false
            AttrOperator.INCLUDES -> {
                // [attr~="word"]: must contain whole word (space separated)
                if (actual.split("""\s+""".toRegex()).none { it == attrsel.value }) return false
            }

            AttrOperator.PREFIX -> if (!actual.startsWith(attrsel.value)) return false
            AttrOperator.SUFFIX -> if (!actual.endsWith(attrsel.value)) return false
            AttrOperator.SUBSTRING -> if (!actual.contains(attrsel.value)) return false
            AttrOperator.DASHMATCH -> {
                // [attr|=val]: actual == val OR starts with val + '-'
                if (!(actual == attrsel.value || actual.startsWith(attrsel.value + "-"))) return false
            }
        }
    }
    return true
}

fun Node?.siblings(selector: String? = null): List<Node> {
    if (this == null) return emptyList()
    val parent = this.parentNode ?: return emptyList()
    val children = parent.childNodes
    return (0 until children.length)
        .asSequence()
        .map { children.item(it) }
        .filter { it.nodeType == Node.ELEMENT_NODE && it != this }
        .filter { selector == null || (it is Element && it.matches(parseSimpleSelector(selector))) }
        .toList()
}

fun Node?.next(selector: String? = null): Node? {
    if (this == null) return null
    val nextElem = generateSequence(this.nextSibling) { it.nextSibling }
        .firstOrNull { it.nodeType == Node.ELEMENT_NODE } ?: return null
    return if (selector == null) nextElem
    else if (nextElem is Element && nextElem.matches(parseSimpleSelector(selector))) nextElem
    else null
}

fun Node?.prev(selector: String? = null): Node? {
    if (this == null) return null
    return generateSequence(this.previousSibling) { it.previousSibling }
        .firstOrNull { it.nodeType == Node.ELEMENT_NODE }
        ?.let { node ->
            if (selector == null) node
            else if (node is Element && node.matches(parseSimpleSelector(selector))) node
            else null
        }
}

fun Node?.parents(selector: String? = null): List<Node> =
    generateSequence(this?.parentNode) { it.parentNode }
        .filter { it.nodeType == Node.ELEMENT_NODE }
        .filter { selector == null || (it is Element && it.matches(parseSimpleSelector(selector))) }
        .toList()