package dev.milkei.audiofocusmixer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PinnedOrderTest {
    private val apps = listOf("youtube", "browser", "music", "game", "podcast")

    @Test
    fun enabledItemsMoveFirstWhileRelativeOrderIsPreserved() {
        val displayed = pinnedFirstStable(apps, setOf("game", "music")) { it }
        assertEquals(listOf("music", "game", "youtube", "browser", "podcast"), displayed)
    }

    @Test
    fun noEnabledItemsDoesNotCreateAnotherList() {
        assertSame(apps, pinnedFirstStable(apps, emptySet()) { it })
    }

    @Test
    fun liveToggleDoesNotMoveRowsUntilSnapshotChanges() {
        // A user toggled "game", but the screen's frozen snapshot is still "music".
        val frozenSnapshot = setOf("music")
        val before = pinnedFirstStable(apps, frozenSnapshot) { it }
        val afterToggle = pinnedFirstStable(apps, frozenSnapshot) { it }
        assertEquals(before, afterToggle)
        assertEquals(listOf("music", "youtube", "browser", "game", "podcast"), before)

        // Only a deliberate refresh or new screen session takes a new snapshot.
        val afterRefresh = pinnedFirstStable(apps, setOf("game", "music")) { it }
        assertEquals(listOf("music", "game", "youtube", "browser", "podcast"), afterRefresh)
    }

    @Test
    fun disabledOrMissingPinnedAppDoesNotAffectOtherOrder() {
        assertEquals(apps, pinnedFirstStable(apps, setOf("uninstalled")) { it })
    }
}
