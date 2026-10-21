package com.markvoronin.immichswipe.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
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
}
