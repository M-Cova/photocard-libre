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
        val resourceDirectories = mapOf(
            "it" to "values-it", "en" to "values-en", "es" to "values-es",
            "de" to "values-de", "fr" to "values-fr", "pt" to "values-pt",
            "ar" to "values-ar", "zh-Hans" to "values-zh-rCN", "ja" to "values-ja",
            "hi" to "values-hi", "id" to "values-in",
        )
        resourceDirectories.forEach { (language, directory) ->
            val localized = resources(File(root, "$directory/strings.xml"))
            assertEquals("Missing or extra strings in $language", base.keys, localized.keys)
            base.forEach { (key, baseElement) ->
                val translated = localized.getValue(key)
                assertEquals("Wrong resource type for $language/$key", baseElement.tagName, translated.tagName)
                if (baseElement.tagName == "plurals") {
                    val items = translated.getElementsByTagName("item")
                    val quantities = (0 until items.length).map {
                        (items.item(it) as Element).getAttribute("quantity")
                    }
                    assertTrue("Missing fallback plural in $language/$key", "other" in quantities)
                }
                assertTrue("Empty string in $language/$key", translated.textContent.isNotBlank())
            }
        }
    }

    @Test
    fun betaFourInfoAndSupportedFormatResourcesExistInEveryLanguage() {
        val required = setOf(
            "app_tagline", "app_description", "app_use_aac", "app_gallery_description",
            "app_support_limit", "supported_formats", "supported_formats_label",
            "supported_formats_value", "open_source_description", "software_license_value",
            "graphic_resources_license_value", "ai_assistance_description", "project_label",
            "credits_description",
        )
        File("src/main/res").listFiles()
            .orEmpty()
            .filter { it.isDirectory && it.name.startsWith("values-") }
            .forEach { directory ->
                assertTrue(
                    "Missing Beta 4 resources in ${directory.name}",
                    resources(File(directory, "strings.xml")).keys.containsAll(required),
                )
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
