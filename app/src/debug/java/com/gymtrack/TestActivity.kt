package com.gymtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import dagger.hilt.android.AndroidEntryPoint

/**
 * Minimal activity used by instrumented tests that need to render a Composable
 * in isolation with Hilt DI, without going through GymTrackNavGraph.
 */
@AndroidEntryPoint
class TestActivity : ComponentActivity() {

    // Content is set by each test via composeTestRule.setContent {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
