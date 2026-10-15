package dev.milkei.audiofocusmixer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppOpModeTest {
    @Test
    fun ignoredAppOpIsDetected() {
        assertEquals(
            AppOpMode.IGNORE,
            AppOpMode.parseRead("OK:TAKE_AUDIO_FOCUS: ignore; time=+2m0s"),
        )
    }

    @Test
    fun absentOverrideUsesDefault() {
        assertEquals(AppOpMode.DEFAULT, AppOpMode.parseRead("OK:No operations."))
    }

    @Test
    fun explicitAllowedModeIsDetected() {
        assertEquals(
            AppOpMode.ALLOW,
            AppOpMode.parseRead("OK:TAKE_AUDIO_FOCUS: allow"),
        )
    }

    @Test
    fun errorsMustNotLookLikeSuccess() {
        assertThrows(IllegalStateException::class.java) {
            AppOpMode.parseRead("ERR:Permission denied")
        }
    }

    @Test
    fun unrecognizedOutputFailsClosed() {
        assertThrows(IllegalStateException::class.java) {
            AppOpMode.parseRead("OK:unexpected response")
        }
    }
}
