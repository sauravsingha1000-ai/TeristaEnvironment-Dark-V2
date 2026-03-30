package com.terista.environment.view.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.niunaijun.blackbox.BlackBoxCore

class ShortcutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pkg = intent.getStringExtra("pkg")
        val userID = intent.getIntExtra("userId", 0)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    BlackBoxCore.get().launchApk(pkg, userID)
                } catch (e: Exception) {
                    android.util.Log.e("ShortcutActivity", "Launch failed: ${e.message}")
                }
            }
            finish()
        }
    }
}
