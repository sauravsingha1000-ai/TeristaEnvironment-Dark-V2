package com.terista.environment.view.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.terista.environment.R

class LogsFragment : Fragment() {
    private var tvLogs: TextView? = null
    private var logScroll: ScrollView? = null
    private val logBuffer = StringBuilder()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_logs, container, false)
        tvLogs = root.findViewById(R.id.tv_logs)
        logScroll = root.findViewById(R.id.log_scroll)
        root.findViewById<TextView>(R.id.btn_clear_log_view)?.setOnClickListener {
            logBuffer.clear()
            tvLogs?.text = "[ Logs cleared ]\n"
        }
        appendLog("[ System log output initialized ]")
        return root
    }

    fun appendLog(message: String) {
        try {
            logBuffer.appendLine(message)
            tvLogs?.text = logBuffer.toString()
            logScroll?.post { logScroll?.fullScroll(View.FOCUS_DOWN) }
        } catch (e: Exception) {
            Log.e("LogsFragment", "Error appending log: ${e.message}")
        }
    }
}
