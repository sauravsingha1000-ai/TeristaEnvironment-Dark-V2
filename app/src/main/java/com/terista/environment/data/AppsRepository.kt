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

    private fun safeLoadAppLabel(applicationInfo: ApplicationInfo): String {
        return try {
            BlackBoxCore.getPackageManager().getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load label for ${applicationInfo.packageName}: ${e.message}")
            applicationInfo.packageName
        }
    }

    private fun safeLoadAppIcon(applicationInfo: ApplicationInfo): android.graphics.drawable.Drawable? {
        return try {
            if (MemoryManager.shouldSkipIconLoading()) {
                Log.w(TAG, "Memory high, skipping icon for ${applicationInfo.packageName}")
                return null
            }

            val icon = BlackBoxCore.getPackageManager().getApplicationIcon(applicationInfo)

            if (icon is android.graphics.drawable.BitmapDrawable) {
                val bitmap = icon.bitmap
                if (bitmap.width > 96 || bitmap.height > 96) {
                    val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, 96, 96, true)
                    android.graphics.drawable.BitmapDrawable(
                        BlackBoxCore.getPackageManager()
                            .getResourcesForApplication(applicationInfo.packageName),
                        scaled
                    )
                } else icon
            } else icon
        } catch (e: Exception) {
            Log.w(TAG, "Icon load failed: ${e.message}")
            null
        }
    }

    // ===========================
    // 🔥 FIXED INSTALL METHOD
    // ===========================
    fun installApk(source: String, userId: Int, resultLiveData: MutableLiveData<String>) {
        try {
            val blackBoxCore = BlackBoxCore.get()

            Log.d(TAG, "Installing source: $source")

            val installResult = when {

                // ✅ FIX 1: HANDLE content:// PROPERLY
                source.startsWith("content://") -> {
                    val uri = Uri.parse(source)

                    val inputStream = BlackBoxCore.getContext()
                        .contentResolver.openInputStream(uri)

                    val tempFile = File(
                        BlackBoxCore.getContext().cacheDir,
                        "temp_install.apk"
                    )

                    inputStream?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    Log.d(TAG, "Converted content URI → file: ${tempFile.absolutePath}")
                    blackBoxCore.installPackageAsUser(tempFile, userId)
                }

                // file://
                source.startsWith("file://") -> {
                    val file = File(Uri.parse(source).path!!)
                    blackBoxCore.installPackageAsUser(file, userId)
                }

                // direct file path
                File(source).exists() -> {
                    val file = File(source)
                    blackBoxCore.installPackageAsUser(file, userId)
                }

                // package name
                else -> {
                    blackBoxCore.installPackageAsUser(source, userId)
                }
            }

            if (installResult.success) {
                Log.d(TAG, "Install success: ${installResult.packageName}")

                updateAppSortList(userId, installResult.packageName, true)
                resultLiveData.postValue(getString(R.string.install_success))

                // ✅ FIX 2: DELAY scanUser (IMPORTANT)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        scanUser()
                    } catch (e: Exception) {
                        Log.e(TAG, "scanUser delayed error: ${e.message}")
                    }
                }, 800) // increased delay

            } else {
                Log.e(TAG, "Install failed: ${installResult.msg}")
                resultLiveData.postValue(getString(R.string.install_fail, installResult.msg))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK: ${e.message}")
            resultLiveData.postValue("Installation failed: ${e.message}")
        }
    }

    // ===========================
    // 🔥 FIXED scanUser (SAFE)
    // ===========================
    private fun scanUser() {
        try {
            val blackBoxCore = BlackBoxCore.get()
            val userList = blackBoxCore.users

            if (userList.isEmpty()) return

            val id = userList.last().id

            val apps = blackBoxCore.getInstalledApplications(0, id)

            // ✅ DO NOT DELETE USER IMMEDIATELY
            if (apps.isEmpty()) {
                Log.w(TAG, "scanUser: empty but skipping delete (race condition protection)")
                return
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in scanUser: ${e.message}")
        }
    }

    // ===========================
    // KEEP EVERYTHING SAME BELOW
    // ===========================

    fun unInstall(packageName: String, userID: Int, resultLiveData: MutableLiveData<String>) {
        try {
            BlackBoxCore.get().uninstallPackageAsUser(packageName, userID)
            updateAppSortList(userID, packageName, false)
            scanUser()
            resultLiveData.postValue(getString(R.string.uninstall_success))
        } catch (e: Exception) {
            Log.e(TAG, "Error uninstalling APK: ${e.message}")
            resultLiveData.postValue("Uninstallation failed: ${e.message}")
        }
    }

    fun launchApk(packageName: String, userId: Int, launchLiveData: MutableLiveData<Boolean>) {
        try {
            val result = BlackBoxCore.get().launchApk(packageName, userId)
            launchLiveData.postValue(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching APK: ${e.message}")
            launchLiveData.postValue(false)
        }
    }

    fun clearApkData(packageName: String, userID: Int, resultLiveData: MutableLiveData<String>) {
        try {
            BlackBoxCore.get().clearPackage(packageName, userID)
            resultLiveData.postValue(getString(R.string.clear_success))
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing APK data: ${e.message}")
            resultLiveData.postValue("Clear failed: ${e.message}")
        }
    }

    private fun updateAppSortList(userID: Int, pkg: String, isAdd: Boolean) {
        try {
            val savedSortList = AppManager.mRemarkSharedPreferences.getString("AppList$userID", "")
            val sortList = linkedSetOf<String>()

            if (savedSortList != null) {
                sortList.addAll(savedSortList.split(","))
            }

            if (isAdd) sortList.add(pkg) else sortList.remove(pkg)

            AppManager.mRemarkSharedPreferences.edit().apply {
                putString("AppList$userID", sortList.joinToString(","))
                apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating app sort list: ${e.message}")
        }
    }

    fun updateApkOrder(userID: Int, dataList: List<AppInfo>) {
        try {
            AppManager.mRemarkSharedPreferences.edit().apply {
                putString("AppList$userID", dataList.joinToString(",") { it.packageName })
                apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating APK order: ${e.message}")
        }
    }
}
