package dev.milkei.audiofocusmixer.core

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** A cancellable, bounded gate for expensive operations. Waiting does not block a thread. */
internal class ConcurrencyGate(maxParallel: Int) {
    init {
        require(maxParallel > 0)
    }

    private val permits = Semaphore(maxParallel)

    suspend fun <T> withSlot(operation: suspend () -> T): T =
        permits.withPermit { operation() }
}
