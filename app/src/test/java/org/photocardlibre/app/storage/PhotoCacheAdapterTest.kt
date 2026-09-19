package org.photocardlibre.app.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoCacheAdapterTest {
    @Test
    fun onlyJpegAndPngAreAcceptedByV01() {
        assertEquals("jpg", PhotoCacheAdapter.extensionForMime("image/jpeg"))
        assertEquals("png", PhotoCacheAdapter.extensionForMime("image/png"))
        assertNull(PhotoCacheAdapter.extensionForMime("image/heic"))
        assertNull(PhotoCacheAdapter.extensionForMime("image/webp"))
        assertNull(PhotoCacheAdapter.extensionForMime(null))
    }
}
