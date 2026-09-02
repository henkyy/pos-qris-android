package com.henky.posqris

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class ProfessionalPosActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
