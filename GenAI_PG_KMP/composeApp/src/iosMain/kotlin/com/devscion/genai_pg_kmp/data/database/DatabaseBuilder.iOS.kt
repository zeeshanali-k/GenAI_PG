package com.devscion.genai_pg_kmp.data.database

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class DatabaseBuilder() {
    actual fun get(): RoomDatabase.Builder<AppDatabase> {
        return getDatabaseBuilder()
    }

    fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
        val dbFilePath = documentDirectory() + "/${AppDatabase.DATABASE_NAME}.db"
        return Room.databaseBuilder<AppDatabase>(
            name = dbFilePath,
            factory = RoomDBConstructor::initialize,
        )
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