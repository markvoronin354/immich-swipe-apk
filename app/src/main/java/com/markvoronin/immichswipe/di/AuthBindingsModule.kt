package com.markvoronin.immichswipe.di

import com.markvoronin.immichswipe.data.repository.AccountRepository
import com.markvoronin.immichswipe.data.repository.AccountSaver
import com.markvoronin.immichswipe.data.repository.AuthRepository
import com.markvoronin.immichswipe.data.repository.CredentialChecker
import com.markvoronin.immichswipe.data.repository.LoginSessionStore
import com.markvoronin.immichswipe.data.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Lets the login screen depend on small interfaces, so its logic can be tested without Android. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindingsModule {
    @Binds abstract fun bindCredentialChecker(impl: AuthRepository): CredentialChecker
    @Binds abstract fun bindLoginSessionStore(impl: SessionRepository): LoginSessionStore
    @Binds abstract fun bindAccountSaver(impl: AccountRepository): AccountSaver
}
