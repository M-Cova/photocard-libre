package org.photocardlibre.app

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

class BackupRulesTest {
    @Test
    fun manifestKeepsBackupDisabledAndReferencesBothRuleFormats() {
        val application = document(File("src/main/AndroidManifest.xml"))
            .getElementsByTagName("application")
            .item(0) as Element

        assertEquals("false", application.androidAttribute("allowBackup"))
        assertEquals("@xml/backup_rules", application.androidAttribute("fullBackupContent"))
        assertEquals(
            "@xml/data_extraction_rules",
            application.androidAttribute("dataExtractionRules"),
        )
    }

    @Test
    fun legacyRulesExcludeEveryApplicationDataDomain() {
        val rules = document(File("src/main/res/xml/backup_rules.xml"))

        assertEquals("full-backup-content", rules.documentElement.tagName)
        assertNoIncludes(rules)
        assertCompleteExclusions(rules.documentElement)
    }

    @Test
    fun android12RulesExcludeEveryDomainFromCloudAndDeviceTransfer() {
        val rules = document(File("src/main/res/xml/data_extraction_rules.xml"))

        assertEquals("data-extraction-rules", rules.documentElement.tagName)
        assertNoIncludes(rules)
        assertCompleteExclusions(rules.singleElement("cloud-backup"))
        assertCompleteExclusions(rules.singleElement("device-transfer"))
    }

    private fun assertCompleteExclusions(parent: Element) {
        val children = parent.childNodes
        val exclusions = (0 until children.length)
            .mapNotNull { children.item(it) as? Element }
        assertEquals(EXCLUDED_DOMAINS.size, exclusions.size)
        assertEquals(setOf("exclude"), exclusions.map { it.tagName }.toSet())
        assertEquals(EXCLUDED_DOMAINS, exclusions.map { it.getAttribute("domain") }.toSet())
        assertEquals(setOf("."), exclusions.map { it.getAttribute("path") }.toSet())
    }

    private fun assertNoIncludes(document: Document) {
        assertEquals(0, document.getElementsByTagName("include").length)
    }

    private fun Document.singleElement(tagName: String): Element {
        val elements = getElementsByTagName(tagName)
        assertEquals("Expected one <$tagName> section", 1, elements.length)
        return elements.item(0) as Element
    }

    private fun Element.androidAttribute(name: String): String {
        val value = getAttributeNS(ANDROID_NAMESPACE, name)
        assertFalse("Missing android:$name", value.isEmpty())
        return value
    }

    private fun document(file: File): Document =
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(file)

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        val EXCLUDED_DOMAINS = setOf(
            "root",
            "file",
            "database",
            "sharedpref",
            "external",
            "device_root",
            "device_file",
            "device_database",
            "device_sharedpref",
        )
    }
}
