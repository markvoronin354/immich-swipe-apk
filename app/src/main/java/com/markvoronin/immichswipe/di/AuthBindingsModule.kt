package com.markvoronin.immichswipe.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Lets the login screen depend on small interfaces, so its logic can be tested without Android. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindingsModule
