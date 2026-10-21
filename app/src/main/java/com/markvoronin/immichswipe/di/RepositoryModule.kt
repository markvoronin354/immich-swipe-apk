package com.markvoronin.immichswipe.di

import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.data.local.dao.AlbumAssetDao
import com.markvoronin.immichswipe.data.repository.AlbumRepository
import com.markvoronin.immichswipe.data.repository.AssetRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Only the repositories whose constructors have optional test parameters are provided by hand.
 * Everything else (session, account, auth, user, swipe decision) is created through its own
 * @Inject constructor.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAlbumRepository(sessionManager: SessionManager): AlbumRepository {
        return AlbumRepository(sessionManager = sessionManager)
    }

    @Provides
    @Singleton
    fun provideAssetRepository(
        sessionManager: SessionManager,
        albumAssetDao: AlbumAssetDao
    ): AssetRepository {
        return AssetRepository(sessionManager = sessionManager, albumAssetDao = albumAssetDao)
    }
}
