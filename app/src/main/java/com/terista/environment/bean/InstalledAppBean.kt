package com.terista.environment.bean

import android.graphics.drawable.Drawable


data class InstalledAppBean(val name:String, val icon: Drawable?, val packageName:String, val sourceDir:String, val isInstall:Boolean)
