package com.stack.player

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

internal class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 32f
            setPadding(64, 128, 64, 64)
        }
        setContentView(textView)
    }
}
