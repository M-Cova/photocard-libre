package org.photocardlibre.app.python

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PythonAlbumBridgeLoggingTest {
    private val sensitiveMessage =
        "content://private/42 /data/user/0/photo.jpg DIDASCALIA RISERVATA"

    @Test
    fun bridgeFailureIsReturnedWithoutCrashingTheCaller() {
        val result = captureBridgeResult<String> {
            throw IllegalStateException(sensitiveMessage)
        }

        assertTrue(result.isFailure)
        assertEquals(sensitiveMessage, result.exceptionOrNull()?.message)
    }

    @Test
    fun releaseLoggingUsesOnlyAGenericMessageWithoutThrowable() {
        val log = bridgeFailureLog(IllegalStateException(sensitiveMessage), isDebug = false)

        assertEquals("Python bridge operation failed", log.message)
        assertNull(log.throwable)
        assertTrue(sensitiveMessage.split(" ").none(log.message::contains))
    }

    @Test
    fun debugLoggingCanRetainTheThrowableForDiagnostics() {
        val error = IllegalStateException(sensitiveMessage)

        val log = bridgeFailureLog(error, isDebug = true)

        assertEquals("Python bridge operation failed", log.message)
        assertSame(error, log.throwable)
    }

    @Test
    fun pythonErrorTypeIsDiagnosticOnly() {
        assertNull(pythonFailureLogMessage("ImageLoadError", isDebug = false))
        assertEquals(
            "Python bridge operation failed (ImageLoadError)",
            pythonFailureLogMessage("ImageLoadError", isDebug = true),
        )
    }
}
