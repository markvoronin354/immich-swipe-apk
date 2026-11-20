package com.markvoronin.immichswipe.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Migration tests run against the schemas exported to app/schemas.
 *
 * Only version 13 is exported, so older migrations cannot be replayed. For every future migration
 * N -> N+1 add a test like [schemaVersion13IsValid]: create the old version with the helper, insert
 * representative rows, run `runMigrationsAndValidate(..., MIGRATION)` and assert the data survived.
 *
 * Do NOT run these through `connectedAndroidTest` on a phone with real app data: Gradle uninstalls the
 * app afterwards, which deletes its data. Use an emulator or a Gradle managed device.
 */
class AppDatabaseMigrationTest {

    private val testDbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun schemaVersion13IsValid() {
        helper.createDatabase(testDbName, 13).apply {
            execSQL(
                "INSERT INTO swipe_decisions (assetId, albumId, userId, decision, createdAt, isSynced, wasSyncedSkip) " +
                    "VALUES ('a1', 'album', 'u1', 'KEEP', 1, 0, 0)"
            )
            close()
        }
        // Reopening at the same version validates the schema against the exported definition.
        val db = helper.runMigrationsAndValidate(testDbName, 13, true)
        db.query("SELECT decision FROM swipe_decisions WHERE assetId = 'a1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("KEEP", cursor.getString(0))
        }
    }

    @Test
    fun migrate13To14KeepsAccountsAndStartsWithoutHeaders() {
        helper.createDatabase(testDbName, 13).apply {
            execSQL(
                "INSERT INTO user_accounts (userId, baseUrl, apiKey, userName, userEmail, avatarColor, lastActive) " +
                    "VALUES ('u1', 'https://photos.example.com', 'enc1:abc', 'Ann', 'a@b.c', 'blue', 5)"
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(testDbName, 14, true, AppDatabase.MIGRATION_13_14)
        db.query("SELECT baseUrl, apiKey, userName, lastActive, customHeaders FROM user_accounts WHERE userId = 'u1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("https://photos.example.com", cursor.getString(0))
            assertEquals("enc1:abc", cursor.getString(1))
            assertEquals("Ann", cursor.getString(2))
            assertEquals(5L, cursor.getLong(3))
            assertTrue(cursor.isNull(4))
        }
        // The new column accepts a value.
        db.execSQL("UPDATE user_accounts SET customHeaders = 'enc1:xyz' WHERE userId = 'u1'")
        db.query("SELECT customHeaders FROM user_accounts WHERE userId = 'u1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("enc1:xyz", cursor.getString(0))
        }
    }

    @Test
    fun migrate14To15ClearsTheAssetCacheButKeepsDecisions() {
        helper.createDatabase(testDbName, 14).apply {
            execSQL("INSERT INTO album_assets (albumId, assetId, userId) VALUES ('album', 'a1', 'u1')")
            execSQL(
                "INSERT INTO swipe_decisions (assetId, albumId, userId, decision, createdAt, isSynced, wasSyncedSkip) " +
                    "VALUES ('a1', 'album', 'u1', 'KEEP', 1, 0, 0)"
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(testDbName, 15, true, AppDatabase.MIGRATION_14_15)
        db.query("SELECT COUNT(*) FROM album_assets").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        db.query("SELECT decision FROM swipe_decisions WHERE assetId = 'a1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("KEEP", cursor.getString(0))
        }
        // The new column accepts a value.
        db.execSQL("INSERT INTO album_assets (albumId, assetId, userId, livePhotoVideoId) VALUES ('album', 'a2', 'u1', 'clip')")
    }
}
