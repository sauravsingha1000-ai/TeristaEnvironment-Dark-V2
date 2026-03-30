package com.terista.environment.view.splash

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.terista.environment.R
import com.terista.environment.util.InjectionUtil
import com.terista.environment.view.list.ListViewModel
import com.terista.environment.view.main.MainActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.logo)

        // 🔥 PRELOAD APPS (FIX FOR EMPTY LIST)
        val viewModel = ViewModelProvider(
            this,
            InjectionUtil.getListFactory()
        ).get(ListViewModel::class.java)

        viewModel.previewInstalledList()

        // Fade animation
        val fade = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f)
        fade.duration = 800

        // Scale animation
        val scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.8f, 1.1f)
        val scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.8f, 1.1f)

        scaleX.duration = 1200
        scaleY.duration = 1200

        scaleX.interpolator = AccelerateDecelerateInterpolator()
        scaleY.interpolator = AccelerateDecelerateInterpolator()

        fade.start()
        scaleX.start()
        scaleY.start()

        // Navigate to Main
        logo.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1500)
    }
}
