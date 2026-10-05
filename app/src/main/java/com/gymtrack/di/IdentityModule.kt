package com.gymtrack.di

import com.gymtrack.data.identity.GoogleCredentialProviderImpl
import com.gymtrack.data.repository.GoogleIdentityRepositoryImpl
import com.gymtrack.domain.identity.GoogleCredentialProvider
import com.gymtrack.domain.repository.GoogleIdentityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IdentityModule {

    @Binds
    @Singleton
    abstract fun bindGoogleCredentialProvider(
        impl: GoogleCredentialProviderImpl,
    ): GoogleCredentialProvider

    @Binds
    @Singleton
    abstract fun bindGoogleIdentityRepository(
        impl: GoogleIdentityRepositoryImpl,
    ): GoogleIdentityRepository
}
