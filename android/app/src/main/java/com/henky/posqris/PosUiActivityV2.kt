package com.henky.posqris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class PosUiActivityV2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OwnerPosApp() }
    }
}
