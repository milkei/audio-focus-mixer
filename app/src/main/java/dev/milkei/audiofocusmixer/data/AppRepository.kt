package dev.milkei.audiofocusmixer.data

import android.content.Context
import android.content.pm.ApplicationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val priority: Int,
)

object AppRepository {
    suspend fun load(context: Context): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        pm.getInstalledApplications(0)
            .asSequence()
            .filter { it.packageName != context.packageName }
            .map { app ->
                val system = app.flags and ApplicationInfo.FLAG_SYSTEM != 0
                val updated = app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
                InstalledApp(
                    packageName = app.packageName,
                    label = pm.getApplicationLabel(app).toString(),
                    isSystem = system && !updated,
                    priority = priority(app),
                )
            }
            .sortedWith(compareBy(InstalledApp::priority, { it.label.lowercase() }))
            .toList()
    }

    private fun priority(info: ApplicationInfo): Int {
        if (info.packageName in MEDIA_PACKAGES) return 0
        return when (info.category) {
            ApplicationInfo.CATEGORY_AUDIO -> 1
            ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_GAME -> 2
            else -> 3
        }
    }

    private val MEDIA_PACKAGES = setOf(
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music",
        "com.spotify.music",
        "com.apple.android.music",
        "org.videolan.vlc",
        "com.netflix.mediaclient",
        "com.sec.android.app.music",
        "com.soundcloud.android",
    )
}
