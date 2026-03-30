package com.terista.environment.view.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import top.niunaijun.blackbox.BlackBoxCore
import com.terista.environment.R
import com.terista.environment.app.App
import com.terista.environment.databinding.ActivityMainBinding
import com.terista.environment.util.Resolution
import com.terista.environment.util.inflate
import com.terista.environment.view.apps.AppsFragment
import com.terista.environment.view.base.LoadingActivity
import com.terista.environment.view.fake.FakeManagerActivity
import com.terista.environment.view.list.ListActivity
import com.terista.environment.view.setting.SettingActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : LoadingActivity() {

    private val viewBinding: ActivityMainBinding by inflate()
    private lateinit var appsFragment: AppsFragment
    private var currentUser = 0

    companion object {
        private const val TAG = "MainActivity"
        fun start(context: Context) {
            context.startActivity(Intent(context, MainActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            BlackBoxCore.get().onBeforeMainActivityOnCreate(this)
            setContentView(viewBinding.root)

            ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                viewBinding.headerLayout.setPadding(
                    viewBinding.headerLayout.paddingLeft,
                    systemBars.top + 8,
                    viewBinding.headerLayout.paddingRight,
                    viewBinding.headerLayout.paddingBottom
                )
                insets
            }

            BlackBoxCore.get().onAfterMainActivityOnCreate(this)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                if (android.os.Environment.isExternalStorageManager()) loadSingleFragment()
                else showStoragePermissionDialog()
            } else {
                loadSingleFragment()
            }

            initHeaderButtons()
            initFab()
            initBottomNav()
            updateSubtitle()

        } catch (e: Exception) {
            Log.e(TAG, "Critical error in onCreate: ${e.message}")
            showErrorDialog("Failed to initialize app: ${e.message}")
        }
    }

    private fun initHeaderButtons() {
        // Settings icon in header
        viewBinding.btnSettings.setOnClickListener {
            SettingActivity.start(this)
        }
    }

    private fun initFab() {
        viewBinding.fab.setOnClickListener {
            val intent = Intent(this, ListActivity::class.java)
            intent.putExtra("userID", currentUser)
            apkPathResult.launch(intent)
        }
    }

    private fun initBottomNav() {
        viewBinding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_apps -> {
                    if (::appsFragment.isInitialized) loadFragment(appsFragment)
                    viewBinding.tvTitle.text = "Terista"
                    viewBinding.fab.visibility = View.VISIBLE
                    true
                }
                R.id.nav_running -> {
                    loadFragment(RunningFragment())
                    viewBinding.tvTitle.text = "Running"
                    viewBinding.fab.visibility = View.GONE
                    true
                }
                R.id.nav_logs -> {
                    loadFragment(LogsFragment())
                    viewBinding.tvTitle.text = "Logs"
                    viewBinding.fab.visibility = View.GONE
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading fragment: ${e.message}")
        }
    }

    private fun updateSubtitle() {
        try {
            val count = try {
                BlackBoxCore.get().getInstalledPackages(0, currentUser)?.size ?: 0
            } catch (e: Exception) { 0 }
            viewBinding.tvSubtitle.text = "$count apps installed"
        } catch (e: Exception) {
            viewBinding.tvSubtitle.text = "0 apps installed"
        }
    }

    private fun showStoragePermissionDialog() {
        MaterialDialog(this).show {
            title(text = "Storage Permission Required")
            message(text = "This app needs All Files Access permission to show and install apps properly.\n\nPlease allow it in the next screen.")
            positiveButton(text = "Grant Permission") { openStorageSettings() }
            negativeButton(text = "Cancel") { loadSingleFragment() }
        }
    }

    private fun openStorageSettings() {
        val intent = try {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
        } catch (e: Exception) {
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        }
        storagePermissionLauncher.launch(intent)
    }

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                loadSingleFragment()
            }
        }

    override fun onResume() {
        super.onResume()
        updateSubtitle()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (android.os.Environment.isExternalStorageManager()) {
                if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
                    loadSingleFragment()
                }
            }
        }
    }

    private fun loadSingleFragment() {
        try {
            currentUser = 0
            if (!::appsFragment.isInitialized) {
                appsFragment = AppsFragment.newInstance(currentUser)
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, appsFragment)
                .commit()
            viewBinding.bottomNav.selectedItemId = R.id.nav_apps
        } catch (e: Exception) {
            Log.e(TAG, "Error loading fragment: ${e.message}")
        }
    }

    fun showFloatButton(show: Boolean) {
        val tranY = Resolution.convertDpToPixel(120F, App.getContext())
        val time = 200L
        if (show) viewBinding.fab.animate().translationY(0f).alpha(1f).setDuration(time).start()
        else viewBinding.fab.animate().translationY(tranY).alpha(0f).setDuration(time).start()
    }

    private val apkPathResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.let { data ->
                    val source = data.getStringExtra("source")
                    if (source != null && ::appsFragment.isInitialized) {
                        appsFragment.installApk(source)
                        // Update subtitle after install
                        viewBinding.bottomNav.postDelayed({ updateSubtitle() }, 1500)
                    }
                }
            }
        }

    private fun showErrorDialog(message: String) {
        MaterialDialog(this).show {
            title(text = "Error")
            message(text = message)
            positiveButton(text = "OK") { finish() }
        }
    }
}
