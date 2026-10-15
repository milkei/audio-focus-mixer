package dev.milkei.audiofocusmixer.core

import android.content.Context
import dev.milkei.audiofocusmixer.privileged.ShizukuClient
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

    fun trackedCount(): Int = store.trackedPackages().size

    suspend fun setEnabled(packageName: String, enabled: Boolean): AppOpMode =
        mutationMutex.withLock {
            val current = read(packageName)
            val target = if (enabled) AppOpMode.IGNORE
                else store.original(packageName) ?: AppOpMode.DEFAULT

            if (enabled && current != AppOpMode.IGNORE && store.original(packageName) == null) {
                // Commit the backup BEFORE touching Android AppOps.
                store.record(packageName, current)
            }

            if (current != target) {
                ShizukuClient.writeMode(packageName, target)
            }

            val verified = read(packageName)
            check(verified == target) {
                "변경 후 시스템 상태가 예상과 달라: " + verified.cli
            }

            if (!enabled && store.original(packageName) != null) {
                store.remove(packageName)
            }
            verified
        }

    suspend fun restoreTracked(): RestoreReport = mutationMutex.withLock {
        var restored = 0
        val failures = mutableListOf<String>()
        for (pkg in store.trackedPackages()) {
            val target = store.original(pkg)
            if (target == null) {
                failures += pkg
                continue
            }
            try {
                val actual = read(pkg)
                if (actual != target) ShizukuClient.writeMode(pkg, target)
                check(read(pkg) == target) { "복원 상태 검증 실패" }
                store.remove(pkg)
                restored++
            } catch (error: Exception) {
                failures += pkg
            }
        }
        RestoreReport(restored, failures)
    }

    data class RestoreReport(val restored: Int, val failedPackages: List<String>)
}
