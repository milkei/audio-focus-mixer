package dev.milkei.audiofocusmixer.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.milkei.audiofocusmixer.core.AppOpMode
import dev.milkei.audiofocusmixer.core.FocusController
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
    var restoring by remember { mutableStateOf(false) }

    val modes = remember { mutableStateMapOf<String, AppOpMode>() }
    val failures = remember { mutableStateMapOf<String, String>() }
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
        } catch (error: Exception) {
            snackbar.showSnackbar("설치 앱 목록 조회에 실패했어: " + (error.message ?: "알 수 없는 오류"))
        } finally {
            loadingApps = false
        }
    }

    LifecycleResumeEffect(Unit) {
        ShizukuClient.refresh()
        reload() // reflect changes made by other AppOps tools while backgrounded
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

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { if (!restoring) showRestoreDialog = false },
            title = { Text("내 변경사항 복원") },
            text = {
                Text(
                    "이 앱에서 믹싱을 켜기 전에 저장한 설정 " +
                        trackedCount + "개를 원래 모드로 복원할까? " +
                        "Volume++ 등 다른 앱이 설정한 항목은 초기화하지 않아."
                )
            },
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
                                    append("복원 " + report.restored + "개")
                                    if (report.skippedExternal > 0) {
                                        append(", 외부 변경 유지 " + report.skippedExternal + "개")
                                    }
                                    if (report.failedPackages.isNotEmpty()) {
                                        append(", 실패 " + report.failedPackages.size + "개")
                                    }
                                }
                                snackbar.showSnackbar(message)
                            } catch (error: Exception) {
                                snackbar.showSnackbar(error.message ?: "복원 도중 오류가 발생했어")
                            } finally {
                                restoring = false
                            }
                        }
                    },
                ) { Text("복원하기") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("취소") }
            },
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                "Audio Focus Mixer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "앱별 오디오 포커스 제어 · 시스템 볼륨 패널 유지",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

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
                    runCatching { context.startActivity(launch) }
                },
            )
            Spacer(Modifier.height(12.dp))

            Text(
                "믹싱을 켜면 이 앱의 오디오 포커스 요청을 차단해 동시 재생을 돕게 돼. " +
                    "변경 후 해당 앱의 재생을 다시 시작해 줘.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "내가 변경한 설정: " + trackedCount + "개",
                    style = MaterialTheme.typography.labelMedium,
                )
                TextButton(
                    enabled = status.ready && trackedCount > 0 && !restoring,
                    onClick = { showRestoreDialog = true },
                ) { Text("일괄 복원") }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                enabled = status.ready,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("앱 이름 또는 패키지 검색") },
                singleLine = true,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Checkbox(checked = showSystem, onCheckedChange = { showSystem = it })
                Text("시스템 앱 표시", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { reload() }, enabled = status.ready) {
                    Text("상태 새로고침")
                }
            }
            HorizontalDivider()

            if (!status.ready) {
                Text(
                    "Shizuku 연결을 완료하면 앱별 믹싱을 제어할 수 있어.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 20.dp),
                )
            } else if (loadingApps) {
                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("설치 앱 조회 중…", modifier = Modifier.padding(start = 12.dp))
                }
            } else if (shownApps.isEmpty()) {
                Text(
                    "조건에 맞는 앱이 없어.",
                    modifier = Modifier.padding(top = 20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(shownApps, key = InstalledApp::packageName) { app ->
                        val name = app.packageName
                        val mode = modes[name]
                        val error = failures[name]
                        val isBusy = busy[name] == true
                        val requestVersion = version

                        LaunchedEffect(name, status.ready, requestVersion) {
                            if (!status.ready || modes.containsKey(name) || failures.containsKey(name)) {
                                return@LaunchedEffect
                            }
                            try {
                                val actual = controller.read(name)
                                if (version == requestVersion && busy[name] != true) {
                                    modes[name] = actual
                                }
                            } catch (exception: Exception) {
                                if (version == requestVersion && busy[name] != true) {
                                    failures[name] = exception.message ?: "상태 조회 실패"
                                }
                            }
                        }

                        MixerAppRow(
                            app = app,
                            mode = mode,
                            error = error,
                            busy = isBusy || restoring,
                            onRetry = {
                                modes.remove(name)
                                failures.remove(name)
                                reload()
                            },
                            onToggle = { enabled ->
                                if (busy[name] == true || restoring) return@MixerAppRow
                                busy[name] = true
                                scope.launch {
                                    try {
                                        modes[name] = controller.setEnabled(name, enabled)
                                        failures.remove(name)
                                        trackedCount = controller.trackedCount()
                                    } catch (exception: Exception) {
                                        failures[name] = exception.message ?: "설정 변경 실패"
                                        modes.remove(name)
                                        snackbar.showSnackbar(
                                            app.label + ": " + (exception.message ?: "설정 실패")
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
            val heading = when {
                status.ready -> "Shizuku 연결 완료"
                !status.running -> "Shizuku 실행 필요"
                !status.granted -> "Shizuku 권한 필요"
                status.connecting -> "권한 서비스 연결 중"
                else -> "Shizuku 연결 확인 필요"
            }
            Text(heading, fontWeight = FontWeight.SemiBold)
            status.error?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(8.dp))
            when {
                status.ready -> Text(
                    "오디오 포커스 설정을 변경할 수 있어.",
                    style = MaterialTheme.typography.bodySmall,
                )
                !status.running -> OutlinedButton(onClick = onOpenShizuku) {
                    Text("Shizuku 열기")
                }
                !status.granted -> Button(onClick = onRequestPermission) {
                    Text("접근 권한 요청")
                }
                status.connecting -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("연결 중…", modifier = Modifier.padding(start = 10.dp))
                }
                else -> Button(onClick = onRetry) { Text("다시 연결") }
            }
        }
    }
}

@Composable
private fun MixerAppRow(
    app: InstalledApp,
    mode: AppOpMode?,
    error: String?,
    busy: Boolean,
    onRetry: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            when {
                error != null -> TextButton(onClick = onRetry) { Text("상태 조회 실패 · 다시 시도") }
                mode == null -> Text(
                    "실제 상태 확인 중…",
                    style = MaterialTheme.typography.labelSmall,
                )
                mode == AppOpMode.IGNORE -> Text(
                    "오디오 포커스 무시",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                )
                else -> Text(
                    "현재 모드: " + mode.cli,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        if (busy && mode != null) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Switch(
                checked = mode == AppOpMode.IGNORE,
                onCheckedChange = onToggle,
                enabled = !busy && mode != null && error == null,
            )
        }
    }
}
