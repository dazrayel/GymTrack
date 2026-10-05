package com.gymtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.gymtrack.domain.model.ThemeMode
import com.gymtrack.domain.repository.GoogleIdentityRepository
import com.gymtrack.domain.repository.ThemeRepository
import kotlinx.coroutines.launch
import com.gymtrack.presentation.navigation.GymTrackNavGraph
import com.gymtrack.presentation.theme.GymTrackTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeRepository: ThemeRepository

    @Inject
    lateinit var googleIdentityRepository: GoogleIdentityRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            googleIdentityRepository.tryRestoreSilentSignIn(this@MainActivity)
        }
        enableEdgeToEdge()
        setContent {
            val themeMode by themeRepository.themeMode.collectAsStateWithLifecycle(
                initialValue = ThemeMode.Default,
            )
            GymTrackTheme(darkTheme = themeMode.isDark) {
                GymTrackNavGraph()
            }
        }
    }
}
