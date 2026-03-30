package com.terista.environment.view.setting

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import com.afollestad.materialdialogs.MaterialDialog
import top.niunaijun.blackbox.BlackBoxCore
import com.terista.environment.R
import com.terista.environment.app.AppManager
import com.terista.environment.databinding.ActivitySettingBinding
import com.terista.environment.util.inflate
import com.terista.environment.util.toast
import com.terista.environment.view.base.BaseActivity
import com.terista.environment.view.gms.GmsManagerActivity

class SettingActivity : BaseActivity() {

    private val viewBinding: ActivitySettingBinding by inflate()

    companion object {
        private const val TAG = "SettingActivity"
        fun start(context: Context) {
            context.startActivity(Intent(context, SettingActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        viewBinding.btnBack.setOnClickListener { finish() }
        initSwitches()
        initAboutInfo()
        initDangerZone()
    }

    private fun initSwitches() {
        val loader = AppManager.mBlackBoxLoader
        val prefs = getSharedPreferences("TeristaSettings", MODE_PRIVATE)

        // Root Hide → BlackBoxLoader
        viewBinding.switchRootHide.isChecked = loader.hideRoot()
        viewBinding.switchRootHide.setOnCheckedChangeListener { _, checked ->
            loader.invalidHideRoot(checked)
            toast(R.string.restart_module)
        }

        // GMS Spoofing → open GmsManagerActivity
        viewBinding.switchGmsSpoof.isChecked = BlackBoxCore.get().isSupportGms
        viewBinding.switchGmsSpoof.setOnCheckedChangeListener { _, checked ->
            if (checked) GmsManagerActivity.start(this)
            toast(R.string.restart_module)
        }

        // Disable Signature Check → stored locally
        viewBinding.switchSigCheck.isChecked = prefs.getBoolean("disable_sig_check", false)
        viewBinding.switchSigCheck.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("disable_sig_check", checked).apply()
            toast(R.string.restart_module)
        }

        // Use VPN Network → BlackBoxLoader (FIX: was missing before)
        viewBinding.switchVpnNetwork.isChecked = loader.useVpnNetwork()
        viewBinding.switchVpnNetwork.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                // Request VPN permission if enabling
                val vpnIntent = VpnService.prepare(this)
                if (vpnIntent != null) {
                    startActivity(vpnIntent)
                }
            }
            loader.invalidUseVpnNetwork(checked)
            toast(R.string.restart_module)
        }

        // Disable Flag Secure → BlackBoxLoader (FIX: was missing before)
        viewBinding.switchFlagSecure.isChecked = loader.disableFlagSecure()
        viewBinding.switchFlagSecure.setOnCheckedChangeListener { _, checked ->
            loader.invalidDisableFlagSecure(checked)
            toast(R.string.restart_module)
        }

        // GPU Acceleration → stored locally
        viewBinding.switchGpu.isChecked = prefs.getBoolean("gpu_acceleration", true)
        viewBinding.switchGpu.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("gpu_acceleration", checked).apply()
            toast(R.string.restart_module)
        }

        // Performance Monitor → stored locally
        viewBinding.switchPerfMonitor.isChecked = prefs.getBoolean("perf_monitor", false)
        viewBinding.switchPerfMonitor.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("perf_monitor", checked).apply()
        }

        // Keep Alive Daemon → BlackBoxLoader
        viewBinding.switchDaemon.isChecked = loader.daemonEnable()
        viewBinding.switchDaemon.setOnCheckedChangeListener { _, checked ->
            loader.invalidDaemonEnable(checked)
            toast(R.string.restart_module)
        }

        // Debug Logging → stored locally
        viewBinding.switchDebugLogging.isChecked = prefs.getBoolean("debug_logging", false)
        viewBinding.switchDebugLogging.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("debug_logging", checked).apply()
        }
    }

    private fun initAboutInfo() {
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            viewBinding.tvVersion.text = pInfo.versionName ?: "2.0"
        } catch (e: Exception) {
            viewBinding.tvVersion.text = "2.0"
        }
        try {
            val count = BlackBoxCore.get().getInstalledPackages(0, 0)?.size ?: 0
            viewBinding.tvInstalledCount.text = count.toString()
        } catch (e: Exception) {
            viewBinding.tvInstalledCount.text = "0"
        }
    }

    private fun clearAppLogs() {
        try {
            filesDir.listFiles()?.filter {
                it.name.endsWith(".log") || it.name.contains("log")
            }?.forEach { it.delete() }
            externalCacheDir?.listFiles()?.filter {
                it.name.contains("log")
            }?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e(TAG, "clearAppLogs: ${e.message}")
        }
    }

    private fun initDangerZone() {
        viewBinding.btnClearLogs.setOnClickListener {
            MaterialDialog(this).show {
                title(text = "Clear All Logs")
                message(text = "This will delete all stored system logs. Continue?")
                positiveButton(text = "Clear") {
                    clearAppLogs()
                    toast("Logs cleared")
                }
                negativeButton(res = R.string.cancel)
            }
        }

        viewBinding.btnResetSettings.setOnClickListener {
            MaterialDialog(this).show {
                title(text = "Reset Settings")
                message(text = "All settings will be reset to defaults. This cannot be undone.")
                positiveButton(text = "Reset") {
                    try {
                        getSharedPreferences("TeristaSettings", MODE_PRIVATE).edit().clear().apply()
                        AppManager.mBlackBoxLoader.invalidHideRoot(false)
                        AppManager.mBlackBoxLoader.invalidDaemonEnable(false)
                        AppManager.mBlackBoxLoader.invalidUseVpnNetwork(false)
                        AppManager.mBlackBoxLoader.invalidDisableFlagSecure(false)
                        initSwitches()
                        toast("Settings reset")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error resetting settings: ${e.message}")
                    }
                }
                negativeButton(res = R.string.cancel)
            }
        }

        viewBinding.btnSendLogs.setOnClickListener {
            BlackBoxCore.get().sendLogs(
                "Manual Log Upload from Settings",
                true,
                object : BlackBoxCore.LogSendListener {
                    override fun onSuccess() { runOnUiThread { toast("Logs sent successfully") } }
                    override fun onFailure(error: String?) { runOnUiThread { toast("Failed to send logs") } }
                }
            )
            toast("Sending logs...")
        }
    }
}
