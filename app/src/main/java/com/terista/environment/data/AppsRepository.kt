package com.terista.environment.data

import android.content.pm.ApplicationInfo
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import java.io.File
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.utils.AbiUtils
import com.terista.environment.R
import com.terista.environment.app.AppManager
import com.terista.environment.bean.AppInfo
import com.terista.environment.bean.InstalledAppBean
import com.terista.environment.util.MemoryManager
import com.terista.environment.util.getString

class AppsRepository {

    val TAG: String = "AppsRepository"
    private var mInstalledList = mutableListOf<AppInfo>()

    // ---------------- SAFE LABEL ----------------
    private fun safeLoadAppLabel(applicationInfo: ApplicationInfo): String {
        return try {
            BlackBoxCore.getPackageManager().getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Failed label: ${applicationInfo.packageName}")
            applicationInfo.packageName
        }
    }

    // ---------------- SAFE ICON ----------------
    private fun safeLoadAppIcon(applicationInfo: ApplicationInfo)
            : android.graphics.drawable.Drawable? {
        return try {
            if (MemoryManager.shouldSkipIconLoading()) return null
            BlackBoxCore.getPackageManager().getApplicationIcon(applicationInfo)
        } catch (e: Exception) {
            Log.w(TAG, "Failed icon: ${applicationInfo.packageName}")
            null
        }
    }

    // =========================================================
    // ✅ REQUIRED FUNCTION (FIXED)
    // =========================================================
    fun previewInstallList() {
        try {
            synchronized(mInstalledList) {
                val installedApplications =
                    BlackBoxCore.getPackageManager().getInstalledApplications(0)

                val installedList = mutableListOf<AppInfo>()

                for (app in installedApplications) {
                    try {
                        val file = File(app.sourceDir)

                        if ((app.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue
                        if (!AbiUtils.isSupport(file)) continue
                        if (BlackBoxCore.get().isBlackBoxApp(app.packageName)) continue

                        val info = AppInfo(
                            safeLoadAppLabel(app),
                            safeLoadAppIcon(app),
                            app.packageName,
                            app.sourceDir,
                            false
                        )
                        installedList.add(info)

                    } catch (e: Exception) {
                        Log.e(TAG, "preview error: ${e.message}")
                    }
                }

                mInstalledList.clear()
                mInstalledList.addAll(installedList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "previewInstallList crash: ${e.message}")
        }
    }

    // =========================================================
    // ✅ REQUIRED FUNCTION (FIXED)
    // =========================================================
    fun getInstalledAppList(
        userID: Int,
        loadingLiveData: MutableLiveData<Boolean>,
        appsLiveData: MutableLiveData<List<InstalledAppBean>>
    ) {
        try {
            loadingLiveData.postValue(true)

            synchronized(mInstalledList) {
                val core = BlackBoxCore.get()

                val list = mInstalledList.map {
                    InstalledAppBean(
                        it.name,
                        it.icon,
                        it.packageName,
                        it.sourceDir,
                        core.isInstalled(it.packageName, userID)
                    )
                }

                appsLiveData.postValue(list)
                loadingLiveData.postValue(false)
            }

        } catch (e: Exception) {
            Log.e(TAG, "getInstalledAppList error: ${e.message}")
            loadingLiveData.postValue(false)
            appsLiveData.postValue(emptyList())
        }
    }

    // =========================================================
    // ✅ REQUIRED FUNCTION (FIXED)
    // =========================================================
    fun getVmInstallList(
        userId: Int,
        appsLiveData: MutableLiveData<List<AppInfo>>
    ) {
        try {
            val core = BlackBoxCore.get()

            val applicationList = core.getInstalledApplications(0, userId)

            val result = mutableListOf<AppInfo>()

            applicationList.forEach {
                try {
                    val info = AppInfo(
                        safeLoadAppLabel(it),
                        safeLoadAppIcon(it),
                        it.packageName,
                        it.sourceDir ?: "",
                        false
                    )
                    result.add(info)
                } catch (e: Exception) {
                    Log.e(TAG, "vm list error: ${e.message}")
                }
            }

            appsLiveData.postValue(result)

        } catch (e: Exception) {
            Log.e(TAG, "getVmInstallList crash: ${e.message}")
            appsLiveData.postValue(emptyList())
        }
    }

    // =========================================================
    // INSTALL
    // =========================================================
    fun installApk(
        source: String,
        userId: Int,
        resultLiveData: MutableLiveData<String>
    ) {
        try {
            val core = BlackBoxCore.get()

            val result = if (URLUtil.isValidUrl(source)) {
                core.installPackageAsUser(Uri.parse(source), userId)
            } else {
                core.installPackageAsUser(source, userId)
            }

            if (result.success) {
                resultLiveData.postValue(getString(R.string.install_success))

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    scanUser()
                }, 500)

            } else {
                resultLiveData.postValue(
                    getString(R.string.install_fail, result.msg)
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "install error: ${e.message}")
            resultLiveData.postValue("Install failed: ${e.message}")
        }
    }

    fun unInstall(
        packageName: String,
        userID: Int,
        resultLiveData: MutableLiveData<String>
    ) {
        try {
            BlackBoxCore.get().uninstallPackageAsUser(packageName, userID)
            scanUser()
            resultLiveData.postValue(getString(R.string.uninstall_success))
        } catch (e: Exception) {
            Log.e(TAG, "uninstall error: ${e.message}")
        }
    }

    fun launchApk(
        packageName: String,
        userId: Int,
        launchLiveData: MutableLiveData<Boolean>
    ) {
        try {
            launchLiveData.postValue(
                BlackBoxCore.get().launchApk(packageName, userId)
            )
        } catch (e: Exception) {
            launchLiveData.postValue(false)
        }
    }

    fun clearApkData(
        packageName: String,
        userID: Int,
        resultLiveData: MutableLiveData<String>
    ) {
        try {
            BlackBoxCore.get().clearPackage(packageName, userID)
            resultLiveData.postValue(getString(R.string.clear_success))
        } catch (e: Exception) {
            Log.e(TAG, "clear error: ${e.message}")
        }
    }

    // =========================================================
    // SAFE USER SCAN
    // =========================================================
    private fun scanUser() {
        try {
            val core = BlackBoxCore.get()
            val users = core.users
            if (users.isEmpty()) return

            val id = users.last().id

            val apps = core.getInstalledApplications(0, id)

            if (apps.isEmpty()) {
                Log.w(TAG, "Skip delete user (safe)")
                return
            }

        } catch (e: Exception) {
            Log.e(TAG, "scanUser error: ${e.message}")
        }
    }
}
