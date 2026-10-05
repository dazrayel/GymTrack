package com.gymtrack.data.identity

import com.gymtrack.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleOAuthConfig @Inject constructor() {

    val webClientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()

    val isConfigured: Boolean
        get() = webClientId.isNotEmpty()
}
