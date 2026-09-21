package org.photocardlibre.app.settings

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class LanguageResourcesTest {
    @Test
    fun everySupportedLocaleDefinesEveryTranslatableUiResource() {
        val root = File("src/main/res")
        val base = resources(File(root, "values/strings.xml"))
            .filterValues { it.getAttribute("translatable") != "false" }
        listOf("it", "en", "es", "de", "fr", "pt").forEach { language ->
            val localized = resources(File(root, "values-$language/strings.xml"))
            assertEquals("Missing or extra strings in $language", base.keys, localized.keys)
            base.forEach { (key, baseElement) ->
                val translated = localized.getValue(key)
                assertEquals("Wrong resource type for $language/$key", baseElement.tagName, translated.tagName)
                if (baseElement.tagName == "plurals") {
                    val items = translated.getElementsByTagName("item")
                    val quantities = (0 until items.length).map {
                        (items.item(it) as Element).getAttribute("quantity")
                    }
                    assertTrue("Missing plural forms in $language/$key", quantities.containsAll(listOf("one", "other")))
                }
                assertTrue("Empty string in $language/$key", translated.textContent.isNotBlank())
            }
        }
    }

    private fun resources(file: File): Map<String, Element> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val children = document.documentElement.childNodes
        return (0 until children.length)
            .mapNotNull { children.item(it) as? Element }
            .associateBy { it.getAttribute("name") }
    }
}
