package com.gymtrack.domain.repository

import android.content.Context
import com.gymtrack.domain.identity.GoogleSignInResult
import com.gymtrack.domain.model.GoogleUser
import kotlinx.coroutines.flow.StateFlow

interface GoogleIdentityRepository {

    val currentUser: StateFlow<GoogleUser?>

    /** Non-blocking silent restore; leaves user anonymous when nothing is available. */
    suspend fun tryRestoreSilentSignIn(hostContext: Context)

    suspend fun signIn(hostContext: Context): GoogleSignInResult

    suspend fun signOut(hostContext: Context)
}
