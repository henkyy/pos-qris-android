package com.henky.posqris

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfessionalPosActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val update = withContext(Dispatchers.IO) { AppUpdateChecker.check(this@ProfessionalPosActivity) }
            if (update != null) {
                AppUpdateChecker.showUpdateDialog(
                    activity = this@ProfessionalPosActivity,
                    update = update,
                    onDone = { launchPos() }
                )
            } else {
                launchPos()
            }
        }
    }

    private fun launchPos() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
