package dev.milkei.audiofocusmixer.privileged

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import dev.milkei.audiofocusmixer.IAppOpsService
import dev.milkei.audiofocusmixer.core.AppOpMode
import dev.milkei.audiofocusmixer.service.AppOpsUserService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Process-wide Shizuku connection. The shell-UID UserService exposes ONLY
 * validated TAKE_AUDIO_FOCUS AppOps commands, never arbitrary shell execution.
 */
object ShizukuClient {
    /** Stable UI-facing failure categories. Resources supply the user language. */
    enum class Problem {
        NOT_STARTED, PERMISSION_DENIED, PERMISSION_REQUEST_FAILED,
        SERVICE_UNAVAILABLE, DISCONNECTED, TIMEOUT, CONNECTION_FAILED,
    }

    data class Status(
        val running: Boolean = false,
        val granted: Boolean = false,
        val connecting: Boolean = false,
        val ready: Boolean = false,
        val error: Problem? = null,
    )

    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val PERMISSION_REQUEST_CODE = 21960
    private const val CONNECTION_TIMEOUT_MS = 15_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableStatus = MutableStateFlow(Status())
    val status = mutableStatus.asStateFlow()

    private var context: Context? = null
    private var initialized = false
    @Volatile private var service: IAppOpsService? = null
    private var binding = false
    private var failed = false
    private var failure: Problem? = null
    private var timeout: Job? = null

    private val onBinder = Shizuku.OnBinderReceivedListener {
        scope.launch {
            failed = false
            refresh()
        }
    }

    private val onBinderDead = Shizuku.OnBinderDeadListener {
        scope.launch {
            service = null
            binding = false
            failed = false
            cancelTimeout()
            refresh()
        }
    }

    private val onPermission = Shizuku.OnRequestPermissionResultListener { requestCode, result ->
        if (requestCode == PERMISSION_REQUEST_CODE) {
            scope.launch {
                failure = if (result == PackageManager.PERMISSION_GRANTED) null
                    else Problem.PERMISSION_DENIED
                failed = false
                refresh()
            }
        }
    }

    fun initialize(applicationContext: Context) {
        if (initialized) return
        context = applicationContext.applicationContext
        initialized = true
        Shizuku.addBinderReceivedListenerSticky(onBinder)
        Shizuku.addBinderDeadListener(onBinderDead)
        Shizuku.addRequestPermissionResultListener(onPermission)
        refresh()
    }

    /** Idempotent status recheck on app resume; explicit retries clear failed bind state. */
    fun refresh(force: Boolean = false) {
        if (context == null) return

        val running = runCatching {
            Shizuku.pingBinder() && !Shizuku.isPreV11() && Shizuku.getVersion() >= 11
        }.getOrDefault(false)

        if (!running) {
            service = null
            binding = false
            failed = false
            failure = null
            cancelTimeout()
            mutableStatus.value = Status()
            return
        }

        val granted = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

        if (!granted) {
            service = null
            binding = false
            cancelTimeout()
            mutableStatus.value = Status(running = true, error = failure)
            return
        }

        failure = if (failure == Problem.PERMISSION_DENIED) null else failure
        if (service?.asBinder()?.pingBinder() == false) {
            service = null
        }

        if (service == null && !binding && (!failed || force)) {
            connect(force)
        }
        mutableStatus.value = Status(
            running = true,
            granted = true,
            connecting = binding,
            ready = service != null,
            error = failure,
        )
    }

    fun requestPermission() {
        if (!status.value.running) {
            mutableStatus.value = status.value.copy(error = Problem.NOT_STARTED)
            return
        }
        if (status.value.granted) {
            refresh()
            return
        }
        runCatching {
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                failure = Problem.PERMISSION_DENIED
                error("Permission was denied")
            }
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
        }.onFailure {
            if (failure != Problem.PERMISSION_DENIED) failure = Problem.PERMISSION_REQUEST_FAILED
            mutableStatus.value = status.value.copy(error = failure)
        }
    }

    fun retry() {
        failed = false
        failure = null
        refresh(force = true)
    }

    private fun args(): Shizuku.UserServiceArgs {
        val applicationContext = checkNotNull(context)
        return Shizuku.UserServiceArgs(
            ComponentName(applicationContext.packageName, AppOpsUserService::class.java.name)
        ).daemon(false)
            .processNameSuffix("audiofocus")
            .debuggable(false)
            .version(1)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            scope.launch {
                cancelTimeout()
                binding = false
                if (binder != null && binder.pingBinder()) {
                    service = IAppOpsService.Stub.asInterface(binder)
                    failed = false
                    failure = null
                } else {
                    service = null
                    failed = true
                    failure = Problem.SERVICE_UNAVAILABLE
                }
                refresh()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            scope.launch {
                cancelTimeout()
                service = null
                binding = false
                failed = true
                failure = Problem.DISCONNECTED
                refresh()
            }
        }
    }

    private fun connect(force: Boolean) {
        binding = true
        failure = null
        if (force) {
            runCatching { Shizuku.unbindUserService(args(), connection, true) }
        }
        try {
            Shizuku.bindUserService(args(), connection)
            timeout = scope.launch {
                delay(CONNECTION_TIMEOUT_MS)
                if (binding && service == null) {
                    binding = false
                    failed = true
                    failure = Problem.TIMEOUT
                    refresh()
                }
            }
        } catch (exception: Exception) {
            binding = false
            failed = true
            failure = Problem.CONNECTION_FAILED
        }
    }

    private fun cancelTimeout() {
        timeout?.cancel()
        timeout = null
    }

    suspend fun readMode(packageName: String): AppOpMode =
        AppOpMode.parseRead(call { it.readMode(packageName) })

    suspend fun writeMode(packageName: String, mode: AppOpMode) {
        AppOpMode.parseSuccess(call { it.writeMode(packageName, mode.cli) })
    }

    private suspend fun call(block: (IAppOpsService) -> String): String =
        withContext(Dispatchers.IO) {
            val active = service ?: error("Shizuku connection is not ready")
            try {
                block(active)
            } catch (exception: RemoteException) {
                scope.launch {
                    service = null
                    binding = false
                    failed = true
                    failure = Problem.DISCONNECTED
                    refresh()
                }
                throw IllegalStateException("Shizuku connection failed", exception)
            }
        }
}
