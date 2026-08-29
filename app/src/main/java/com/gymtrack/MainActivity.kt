package com.gymtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gymtrack.presentation.navigation.GymTrackNavGraph
import com.gymtrack.presentation.theme.GymTrackTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymTrackTheme {
                GymTrackNavGraph()
            }
        }
    }
}
