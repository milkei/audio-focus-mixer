package dev.milkei.audiofocusmixer.core

/** A value of the Android TAKE_AUDIO_FOCUS app-op, not an audio playback state. */
enum class AppOpMode(val cli: String) {
    DEFAULT("default"),
    ALLOW("allow"),
    IGNORE("ignore"),
    DENY("deny"),
    FOREGROUND("foreground");

    companion object {
        private val OP_LINE = Regex(
            "TAKE_AUDIO_FOCUS\\s*:\\s*([A-Za-z_]+)",
            RegexOption.IGNORE_CASE,
        )

        fun fromCli(value: String): AppOpMode? =
            entries.firstOrNull { it.cli.equals(value, ignoreCase = true) }

        /** Parse the response from AppOpsUserService; fail closed on unknown CLI output. */
        fun parseRead(response: String): AppOpMode {
            val output = parseSuccess(response)
            val rawMode = OP_LINE.find(output)?.groupValues?.get(1)
            if (rawMode != null) {
                return fromCli(rawMode)
                    ?: error("알 수 없는 오디오 포커스 모드: " + rawMode)
            }
            if (output.isBlank() || output.contains("No operations", ignoreCase = true)) {
                return DEFAULT
            }
            error("시스템 오디오 포커스 상태를 해석할 수 없어: " + output.take(140))
        }

        fun parseSuccess(response: String): String {
            if (response.startsWith("ERR:")) {
                error(response.removePrefix("ERR:").take(300))
            }
            if (!response.startsWith("OK:")) {
                error("Shizuku 응답 형식이 올바르지 않아")
            }
            return response.removePrefix("OK:")
        }
    }
}
