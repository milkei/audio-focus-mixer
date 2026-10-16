package dev.milkei.audiofocusmixer.core

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConcurrencyGateTest {
    @Test
    fun limitsSimultaneousOperations() = runBlocking {
        val gate = ConcurrencyGate(2)
        val active = AtomicInteger()
        val peak = AtomicInteger()
        coroutineScope {
            repeat(20) {
                launch(Dispatchers.Default) {
                    gate.withSlot {
                        val running = active.incrementAndGet()
                        peak.updateAndGet { maxOf(it, running) }
                        try {
                            delay(10)
                        } finally {
                            active.decrementAndGet()
                        }
                    }
                }
            }
        }
        assertEquals(2, peak.get())
        assertEquals(0, active.get())
    }

    @Test
    fun cancellationReleasesPermit() = runBlocking {
        val gate = ConcurrencyGate(1)
        val acquired = CompletableDeferred<Unit>()
        val suspended = launch {
            gate.withSlot {
                acquired.complete(Unit)
                awaitCancellation()
            }
        }
        acquired.await()
        suspended.cancelAndJoin()
        var entered = false
        gate.withSlot { entered = true }
        assertTrue(entered)
    }

    @Test
    fun nonPositiveParallelismRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ConcurrencyGate(0)
        }
    }
}
