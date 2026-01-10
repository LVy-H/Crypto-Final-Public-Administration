package com.gov.crypto.document.service

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class AsicServiceTest {

    private val asicService = AsicService()

    @Test
    fun `createContainer should produce valid ASiC-E zip`() {
        val originalFilename = "test.pdf"
        val docBytes = "PDF-Content".toByteArray()
        val sigBytes = "CMS-Signature".toByteArray()

        val zipBytes = asicService.createContainer(originalFilename, docBytes, sigBytes)

        assertNotNull(zipBytes)
        assertTrue(zipBytes.isNotEmpty())

        val zis = ZipInputStream(ByteArrayInputStream(zipBytes))
        
        // 1. Verify Mimetype (Must be first)
        val firstEntry = zis.nextEntry
        assertEquals("mimetype", firstEntry?.name)
        val mimetypeContent = zis.readBytes().toString(Charsets.UTF_8)
        assertEquals("application/vnd.etsi.asic-e+zip", mimetypeContent)
        
        // 2. Verify Document
        var entry = zis.nextEntry
        while (entry != null) {
            if (entry.name == originalFilename) {
                val content = zis.readBytes()
                assertArrayEquals(docBytes, content)
            } else if (entry.name == "META-INF/signature.p7s") {
                val content = zis.readBytes()
                assertArrayEquals(sigBytes, content)
            }
            entry = zis.nextEntry
        }
    }
}
