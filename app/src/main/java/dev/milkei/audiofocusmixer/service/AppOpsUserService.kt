package dev.milkei.audiofocusmixer.service

import android.content.Context
import dev.milkei.audiofocusmixer.IAppOpsService
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Created by Shizuku in a shell-UID process. This is not a regular Android Service.
 * Deliberately supports only TAKE_AUDIO_FOCUS: no generic shell-command IPC.
 */
class AppOpsUserService() : IAppOpsService.Stub() {
    @Suppress("UNUSED_PARAMETER", "unused")
    constructor(context: Context) : this()

    override fun destroy() {
        exitProcess(0)
    }

    override fun readMode(packageName: String): String {
        if (!validPackage(packageName)) return "ERR:Invalid package name"
        return command("appops", "get", packageName, "TAKE_AUDIO_FOCUS")
    }

    override fun writeMode(packageName: String, mode: String): String {
        if (!validPackage(packageName)) return "ERR:Invalid package name"
        if (mode !in ALLOWED_MODES) return "ERR:Invalid mode"
        return command("appops", "set", packageName, "TAKE_AUDIO_FOCUS", mode)
    }

    private fun validPackage(name: String): Boolean = PACKAGE_NAME.matches(name)

    private fun command(vararg arguments: String): String = try {
        val process = ProcessBuilder(*arguments).redirectErrorStream(true).start()
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            "ERR:Command timed out"
        } else {
            val output = process.inputStream.bufferedReader().use { it.readText().trim() }
            if (process.exitValue() == 0) "OK:" + output
            else "ERR:" + (output.ifEmpty { "Command failed" })
        }
    } catch (error: Exception) {
        "ERR:" + (error.message ?: error.javaClass.simpleName)
    }

    private companion object {
        val PACKAGE_NAME = Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")
        val ALLOWED_MODES = setOf("default", "allow", "ignore", "deny", "foreground")
    }
}
