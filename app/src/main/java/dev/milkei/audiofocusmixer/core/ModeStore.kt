package dev.milkei.audiofocusmixer.core

import android.content.Context

/**
 * Stores original modes ONLY for modifications initiated by this app.
 * A synchronous commit records the restore target before changing system AppOps.
 */
class ModeStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("audio_focus_original_modes", Context.MODE_PRIVATE)

    fun original(packageName: String): AppOpMode? =
        preferences.getString(PREFIX + packageName, null)?.let(AppOpMode::fromCli)

    fun record(packageName: String, original: AppOpMode) {
        check(preferences.edit().putString(PREFIX + packageName, original.cli).commit()) {
            "이전 오디오 포커스 설정을 저장하지 못했어"
        }
    }

    fun remove(packageName: String) {
        check(preferences.edit().remove(PREFIX + packageName).commit()) {
            "복원 기록을 정리하지 못했어"
        }
    }

    fun trackedPackages(): List<String> = preferences.all.keys
        .filter { it.startsWith(PREFIX) }
        .map { it.removePrefix(PREFIX) }
        .sorted()

    private companion object {
        const val PREFIX = "previous:"
    }
}
