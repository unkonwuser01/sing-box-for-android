package io.nekohasekai.sfa.custom

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import io.nekohasekai.sfa.Application
import io.nekohasekai.sfa.BuildConfig
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.bg.BoxService
import io.nekohasekai.sfa.database.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Executes the three app-shortcut commands without opening the main activity. */
class ProxyShortcutActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())

    private val vpnPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            scope.launch {
                if (result.resultCode == Activity.RESULT_OK && startProxy() == StartResult.STARTED) {
                    toast(R.string.shortcut_toast_started)
                } else if (result.resultCode != Activity.RESULT_OK) {
                    toast(R.string.shortcut_toast_vpn_denied)
                } else {
                    toast(R.string.shortcut_toast_start_failed)
                }
                finishSoon()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        scope.launch {
            when (command()) {
                Command.START -> startOrRequestPermission()
                Command.STOP -> {
                    BoxService.stop()
                    toast(R.string.shortcut_toast_stopped)
                    finishSoon()
                }
                Command.TOGGLE -> {
                    if (Settings.startedByUser) {
                        BoxService.stop()
                        toast(R.string.shortcut_toast_stopped)
                        finishSoon()
                    } else {
                        startOrRequestPermission()
                    }
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    private suspend fun startOrRequestPermission() {
        when (startProxy()) {
            StartResult.FAILED -> {
                toast(R.string.shortcut_toast_start_failed)
                finishSoon()
            }
            StartResult.STARTED -> {
                toast(R.string.shortcut_toast_started)
                finishSoon()
            }
            StartResult.PERMISSION_REQUESTED -> Unit
        }
    }

    private suspend fun startProxy(): StartResult {
        return try {
            Application.awaitLibboxReady()
            val permissionIntent =
                withContext(Dispatchers.IO) {
                    if (Settings.selectedProfile == -1L) return@withContext StartData(null, false)
                    Settings.rebuildServiceMode()
                    StartData(VpnService.prepare(this@ProxyShortcutActivity), true)
                }
            if (!permissionIntent.hasProfile) return StartResult.FAILED
            if (permissionIntent.vpnPermission != null) {
                vpnPermission.launch(permissionIntent.vpnPermission)
                return StartResult.PERMISSION_REQUESTED
            }
            ContextCompat.startForegroundService(
                this,
                Intent(this, Settings.serviceClass()),
            )
            Settings.startedByUser = true
            StartResult.STARTED
        } catch (_: Exception) {
            StartResult.FAILED
        }
    }

    private fun command(): Command =
        when {
            intent.getStringExtra("proxy_command") == "start" -> Command.START
            intent.getStringExtra("proxy_command") == "stop" -> Command.STOP
            intent.action == "${BuildConfig.APPLICATION_ID}.action.START" -> Command.START
            intent.action == "${BuildConfig.APPLICATION_ID}.action.STOP" -> Command.STOP
            else -> Command.TOGGLE
        }

    private fun toast(message: Int) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun finishSoon() {
        handler.postDelayed({ finishAndRemoveTask() }, 500L)
    }

    private enum class Command { START, STOP, TOGGLE }

    private enum class StartResult { STARTED, PERMISSION_REQUESTED, FAILED }

    private data class StartData(val vpnPermission: Intent?, val hasProfile: Boolean)
}
