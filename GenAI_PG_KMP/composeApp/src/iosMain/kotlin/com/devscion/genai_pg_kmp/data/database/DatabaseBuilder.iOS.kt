package com.devscion.genai_pg_kmp.data.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.NativeSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import sqlitevec.register_sqlite_vec

actual class DatabaseBuilder() {
    actual fun get(): RoomDatabase.Builder<AppDatabase> {
        return getDatabaseBuilder()
    }

    @OptIn(ExperimentalForeignApi::class)
    fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
        register_sqlite_vec()
        val dbFilePath = documentDirectory() + "/${AppDatabase.DATABASE_NAME}.db"
        return Room.databaseBuilder<AppDatabase>(
            name = dbFilePath,
            factory = RoomDBConstructor::initialize,
        ).setDriver(NativeSQLiteDriver())
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun documentDirectory(): String {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        return requireNotNull(documentDirectory?.path)
    }
}