package dev.milkei.audiofocusmixer.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.milkei.audiofocusmixer.R
import dev.milkei.audiofocusmixer.core.AppOpMode
import dev.milkei.audiofocusmixer.core.FocusController
import dev.milkei.audiofocusmixer.data.AppIconRepository
import dev.milkei.audiofocusmixer.data.AppRepository
import dev.milkei.audiofocusmixer.data.InstalledApp
import dev.milkei.audiofocusmixer.privileged.ShizukuClient
import kotlinx.coroutines.launch

@Composable
fun MixerScreen() {
    val context = LocalContext.current
    val controller = remember { FocusController(context.applicationContext) }
    val status by ShizukuClient.status.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var loadingApps by remember { mutableStateOf(true) }
    var query by rememberSaveable { mutableStateOf("") }
    var showSystem by rememberSaveable { mutableStateOf(false) }
    var version by remember { mutableIntStateOf(0) }
    var trackedCount by remember { mutableIntStateOf(controller.trackedCount()) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(false) }

    val modes = remember { mutableStateMapOf<String, AppOpMode>() }
    val failures = remember { mutableStateMapOf<String, Boolean>() }
    val busy = remember { mutableStateMapOf<String, Boolean>() }

    fun reload() {
        modes.clear()
        failures.clear()
        version++
    }

    LaunchedEffect(Unit) {
        ShizukuClient.initialize(context.applicationContext)
        try {
            apps = AppRepository.load(context.applicationContext)
        } catch (_: Exception) {
            snackbar.showSnackbar(context.getString(R.string.error_load_apps))
        } finally {
            loadingApps = false
        }
    }

    LifecycleResumeEffect(Unit) {
        ShizukuClient.refresh()
        reload() // Re-read modes changed by other AppOps clients.
        onPauseOrDispose { }
    }

    LaunchedEffect(status.ready) {
        if (status.ready) reload()
    }

    val shownApps = remember(apps, query, showSystem) {
        apps.filter { app ->
            (showSystem || !app.isSystem) &&
                (query.isBlank() ||
                    app.label.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true))
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.about_title)) },
            text = { Text(stringResource(R.string.about_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showAboutDialog = false
                    runCatching {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/milkei/audio-focus-mixer")
                            )
                        )
                    }
                }) { Text(stringResource(R.string.about_open_source)) }
            },
            dismissButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.about_close))
                }
            },
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { if (!restoring) showRestoreDialog = false },
            title = { Text(stringResource(R.string.restore_title)) },
            text = { Text(stringResource(R.string.restore_description, trackedCount)) },
            confirmButton = {
                Button(
                    enabled = !restoring,
                    onClick = {
                        showRestoreDialog = false
                        restoring = true
                        scope.launch {
                            try {
                                val report = controller.restoreTracked()
                                trackedCount = controller.trackedCount()
                                reload()
                                val message = buildString {
                                    append(context.getString(R.string.restore_count, report.restored))
                                    if (report.skippedExternal > 0) {
                                        append(context.getString(
                                            R.string.restore_skipped,
                                            report.skippedExternal,
                                        ))
                                    }
                                    if (report.failedPackages.isNotEmpty()) {
                                        append(context.getString(
                                            R.string.restore_failed,
                                            report.failedPackages.size,
                                        ))
                                    }
                                }
                                snackbar.showSnackbar(message)
                            } catch (_: Exception) {
                                snackbar.showSnackbar(context.getString(R.string.restore_error))
                            } finally {
                                restoring = false
                            }
                        }
                    },
                ) { Text(stringResource(R.string.restore_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            item(key = "settings", contentType = "settings") {
                Column {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.app_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(18.dp))

                    ConnectionCard(
                        status = status,
                        onRequestPermission = ShizukuClient::requestPermission,
                        onRetry = ShizukuClient::retry,
                        onOpenShizuku = {
                            val launch = context.packageManager
                                .getLaunchIntentForPackage(ShizukuClient.SHIZUKU_PACKAGE)
                                ?: Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/RikkaApps/Shizuku"),
                                )
                            runCatching { context.startActivity(launch) }.onFailure {
                                scope.launch {
                                    snackbar.showSnackbar(
                                        context.getString(R.string.error_open_shizuku)
                                    )
                                }
                            }
                        },
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.intro),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.tracked_count, trackedCount),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        TextButton(
                            enabled = status.ready && trackedCount > 0 && !restoring,
                            onClick = { showRestoreDialog = true },
                        ) { Text(stringResource(R.string.restore_all)) }
                    }

                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        enabled = status.ready,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.search_apps)) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = showSystem,
                            onCheckedChange = { showSystem = it },
                            enabled = status.ready,
                        )
                        Text(
                            stringResource(R.string.show_system_apps),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = { reload() },
                            enabled = status.ready,
                        ) { Text(stringResource(R.string.refresh_states)) }
                    }
                    HorizontalDivider()
                }
            }

            if (!status.ready) {
                item(key = "connectionHint", contentType = "status") {
                    Text(
                        stringResource(R.string.connection_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else if (loadingApps) {
                item(key = "loading", contentType = "status") {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            stringResource(R.string.loading_apps),
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            } else if (shownApps.isEmpty()) {
                item(key = "empty", contentType = "status") {
                    Text(
                        stringResource(R.string.empty_apps),
                        modifier = Modifier.padding(vertical = 24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(
                    shownApps,
                    key = InstalledApp::packageName,
                    contentType = { "app" },
                ) { app ->
                    val name = app.packageName
                    val mode = modes[name]
                    val hasError = failures[name] == true
                    val isBusy = busy[name] == true
                    val requestVersion = version

                    LaunchedEffect(name, status.ready, requestVersion) {
                        if (!status.ready ||
                            modes.containsKey(name) ||
                            failures.containsKey(name)
                        ) {
                            return@LaunchedEffect
                        }
                        try {
                            val actual = controller.read(name)
                            if (version == requestVersion && busy[name] != true) {
                                modes[name] = actual
                            }
                        } catch (_: Exception) {
                            if (version == requestVersion && busy[name] != true) {
                                failures[name] = true
                            }
                        }
                    }

                    MixerAppRow(
                        app = app,
                        mode = mode,
                        hasError = hasError,
                        busy = isBusy || restoring,
                        onRetry = { reload() },
                        onToggle = { enabled ->
                            if (busy[name] == true || restoring) return@MixerAppRow
                            busy[name] = true
                            scope.launch {
                                try {
                                    modes[name] = controller.setEnabled(name, enabled)
                                    failures.remove(name)
                                    trackedCount = controller.trackedCount()
                                } catch (_: Exception) {
                                    failures[name] = true
                                    modes.remove(name)
                                    snackbar.showSnackbar(
                                        context.getString(R.string.error_apply, app.label)
                                    )
                                } finally {
                                    busy.remove(name)
                                }
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }

            item(key = "privacy", contentType = "footer") {
                Text(
                    stringResource(R.string.privacy_note),
                    modifier = Modifier.padding(top = 24.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { showAboutDialog = true }) {
                    Text(stringResource(R.string.about_action))
                }
            }
        }
    }
}

@Composable
private fun ConnectionCard(
    status: ShizukuClient.Status,
    onRequestPermission: () -> Unit,
    onRetry: () -> Unit,
    onOpenShizuku: () -> Unit,
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            val headingId = when {
                status.ready -> R.string.connection_ready
                !status.running -> R.string.connection_missing
                !status.granted -> R.string.connection_permission
                status.connecting -> R.string.connection_connecting
                else -> R.string.connection_check
            }
            Text(
                stringResource(headingId),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            status.error?.let { problem ->
                val messageId = when (problem) {
                    ShizukuClient.Problem.NOT_STARTED ->
                        R.string.shizuku_not_started
                    ShizukuClient.Problem.PERMISSION_DENIED ->
                        R.string.shizuku_permission_denied
                    ShizukuClient.Problem.PERMISSION_REQUEST_FAILED ->
                        R.string.shizuku_request_failed
                    ShizukuClient.Problem.SERVICE_UNAVAILABLE ->
                        R.string.shizuku_bind_failed
                    ShizukuClient.Problem.DISCONNECTED ->
                        R.string.shizuku_disconnected
                    ShizukuClient.Problem.TIMEOUT ->
                        R.string.shizuku_timeout
                    ShizukuClient.Problem.CONNECTION_FAILED ->
                        R.string.shizuku_bind_failed
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(messageId),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(8.dp))
            when {
                status.ready -> Text(
                    stringResource(R.string.connection_ready_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                !status.running -> OutlinedButton(onClick = onOpenShizuku) {
                    Text(stringResource(R.string.open_shizuku))
                }
                !status.granted -> Button(onClick = onRequestPermission) {
                    Text(stringResource(R.string.request_permission))
                }
                status.connecting -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.connecting),
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
                else -> Button(onClick = onRetry) {
                    Text(stringResource(R.string.retry_connection))
                }
            }
        }
    }
}

@Composable
private fun MixerAppRow(
    app: InstalledApp,
    mode: AppOpMode?,
    hasError: Boolean,
    busy: Boolean,
    onRetry: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    // Icon work is asynchronous; recycled list rows keep a fixed-size placeholder.
    val icon by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = app.packageName,
    ) {
        value = AppIconRepository.load(context.applicationContext, app.packageName)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Snapshot into a local value: delegated Compose State cannot be smart-cast.
        val loadedIcon = icon
        if (loadedIcon != null) {
            Image(
                bitmap = loadedIcon,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
        } else {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Text(
                        app.label.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            when {
                hasError -> TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.retry_read_mode))
                }
                mode == null -> Text(
                    stringResource(R.string.reading_mode),
                    style = MaterialTheme.typography.labelSmall,
                )
                mode == AppOpMode.IGNORE -> Text(
                    stringResource(R.string.mode_ignored),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                )
                else -> {
                    val modeName = when (mode) {
                        AppOpMode.DEFAULT -> stringResource(R.string.mode_default)
                        AppOpMode.ALLOW -> stringResource(R.string.mode_allow)
                        AppOpMode.IGNORE -> stringResource(R.string.mode_ignored)
                        AppOpMode.DENY -> stringResource(R.string.mode_deny)
                        AppOpMode.FOREGROUND -> stringResource(R.string.mode_foreground)
                    }
                    Text(
                        stringResource(R.string.mode_current, modeName),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        if (busy && mode != null) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            val description = stringResource(
                if (mode == AppOpMode.IGNORE) R.string.state_on else R.string.state_off,
                app.label,
            )
            Switch(
                checked = mode == AppOpMode.IGNORE,
                onCheckedChange = onToggle,
                enabled = !busy && mode != null && !hasError,
                modifier = Modifier.semantics { contentDescription = description },
            )
        }
    }
}
