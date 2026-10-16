package dev.milkei.audiofocusmixer.core

import android.content.Context
import dev.milkei.audiofocusmixer.privileged.ShizukuClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serialized AppOps mutations. Reading and verifying actual system state is mandatory.
 * This never resets someone else's app-op during a bulk restore.
 */
class FocusController(context: Context) {
    private val store = ModeStore(context)
    private val mutationMutex = Mutex()

    suspend fun read(packageName: String): AppOpMode =
        ShizukuClient.readMode(packageName)

    suspend fun trackedCount(): Int = withContext(Dispatchers.IO) {
        store.trackedPackages().size
    }

    /**
     * Snapshot only the packages changed here, and verify the current AppOps mode.
     * No scan of every installed app and no persistent favorite preference.
     * The UI decides when to apply the snapshot; toggles never reorder a visible list.
     */
    suspend fun activeTrackedPackages(): Set<String> = withContext(Dispatchers.IO) {
        mutationMutex.withLock {
            buildSet {
                for (pkg in store.trackedPackages()) {
                    try {
                        if (read(pkg) == AppOpMode.IGNORE) add(pkg)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // The list remains usable if one package has been removed.
                    }
                }
            }
        }
    }

    // SharedPreferences.commit() is intentionally synchronous to keep restore data safe.
    // Do not perform that disk I/O, or the associated AppOps IPC, on Compose's UI thread.
    suspend fun setEnabled(packageName: String, enabled: Boolean): AppOpMode =
        withContext(Dispatchers.IO) {
            mutationMutex.withLock {
                val current = read(packageName)
                val saved = store.original(packageName)
                val target = if (enabled) AppOpMode.IGNORE
                    else saved ?: AppOpMode.DEFAULT

                if (enabled && current != AppOpMode.IGNORE && saved == null) {
                    store.record(packageName, current)
                }
                if (current != target) {
                    ShizukuClient.writeMode(packageName, target)
                }
                val verified = read(packageName)
                check(verified == target) {
                    "Audio focus mode did not match the requested value: " + verified.cli
                }
                if (!enabled && saved != null) {
                    store.remove(packageName)
                }
                verified
            }
        }

    suspend fun restoreTracked(): RestoreReport = withContext(Dispatchers.IO) {
        mutationMutex.withLock {
            var restored = 0
            var skippedExternal = 0
            val failures = mutableListOf<String>()
            for (pkg in store.trackedPackages()) {
                val target = store.original(pkg)
                if (target == null) {
                    failures += pkg
                    continue
                }
                try {
                    val actual = read(pkg)
                    if (actual != AppOpMode.IGNORE && actual != target) {
                        // Respect an external change made after this app's toggle.
                        store.remove(pkg)
                        skippedExternal++
                        continue
                    }
                    if (actual != target) ShizukuClient.writeMode(pkg, target)
                    check(read(pkg) == target) { "Restore verification failed" }
                    store.remove(pkg)
                    restored++
                } catch (error: Exception) {
                    failures += pkg
                }
            }
            RestoreReport(restored, failures, skippedExternal)
        }
    }

    data class RestoreReport(
        val restored: Int,
        val failedPackages: List<String>,
        val skippedExternal: Int,
    )
}
