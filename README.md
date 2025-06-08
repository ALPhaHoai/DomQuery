---

# DomQuery: XPath & CSS-Style Selectors for XML in Kotlin

**DomQuery** (`org.domquery`) provides lightweight, CSS-style selector querying and utilities for DOM-style (XML/HTML) documents in Kotlin/JVM.  
It includes support for tag, id, class, and attribute selectors, Android bounds parsing, and `querySelector`/`querySelectorAll` compatible with W3C DOM nodes.

---

## Features

- **CSS-like Query Language:**  
  Query XML elements using syntax such as `"LinearLayout > Button#ok.primary[enabled="true"]"`.
- **Support for `id`, `class`, **tag** and attribute selectors**
- **Combinators:**  
  Descendant (`A B`), child (`A > B`), adjacent sibling (`A + B`), general sibling (`A ~ B`).
- **Multiple Selector Groups:**  
  `"Button, EditText"` selects all matching nodes from both groups.
- **Bounds Parsing Utility:**  
  Parses Android-style bounds `[left,top][right,bottom]` and computes center point.
- **Utility Methods:**
    - `Node.querySelector("selector")`
    - `Node.querySelectorAll("selector")`
    - `Element.matches(simpleSelector)`
    - `XmlDocument.querySelector(cssSelector)`
    - `XmlDocument.querySelectorPoint(cssSelector)` (returns center as `Point`)
- **No dependencies except Java XML libraries**

---

## Usage

### 1. Parse and Query an XML Document

```kotlin
import org.domquery.XmlDocument

val xml = """
<hierarchy>
  <node class="android.widget.FrameLayout" bounds="[0,0][100,200]" resource-id="root">
    <node class="android.widget.Button primary" resource-id="ok" enabled="true" bounds="[10,50][60,100]" />
  </node>
</hierarchy>
""".trimIndent()

val doc = XmlDocument(xml)

// Query for a Button with id "ok" and class "primary"
val node = doc.querySelector("node#ok.primary")
// Get the center point of its bounds (Android-style)
val point = doc.querySelectorPoint("node#ok.primary") // returns Point(35,75)
```

### 2. Direct DOM Query

```kotlin
val root = doc.documentElement
val btn = root.querySelector("node[resource-id=\"ok\"]")
```

### 3. All Matching Nodes

```kotlin
val all = root.querySelectorAll("node.primary[enabled=\"true\"]")
```

---

## Selectors Supported

- **Tag**: `node`, `Button`, etc
- **ID**: `#ok` or attribute `resource-id="ok"`
- **Class**: `.primary` (space-separated in XML attribute)
- **Attribute**: `[enabled="true"]`, `[bounds="[10,20][30,40]"]`
- **Combinators**:
    - Descendant: `A B`
    - Child:      `A > B`
    - Adjacent:   `A + B`
    - Sibling:    `A ~ B`
- **Multiple groups**: `"A, B"`

**Example:**  
`FrameLayout > node.primary[enabled="true"]`

---

## Bounds Utility

Android Uiautomator XML outputs element bounds as  
`bounds="[10,20][30,40]"`  
Use `Node.toCenterPoint()` (returns `java.awt.Point`) to get the element's center.

---

## API Overview

### `XmlDocument(xml: String)`
- `.querySelector(cssSelector: String): Node?`
- `.querySelectorPoint(cssSelector: String): Point?`

### `Node`
- `.querySelector(selector: String): Node?`
- `.querySelectorAll(selector: String): List<Node>`
- `.toCenterPoint(): Point?` (for elements with `"bounds"`)

### `Element`
- `.matches(selector: SimpleSelector): Boolean`

---

## Installation

No Maven/Central artifact yet—copy `DomQuery.kt` source into your project.

- **Kotlin/JVM only**
- Requires Java `javax.xml.parsers` (standard on JVM)

---

**Questions/Contributions welcome!**

---