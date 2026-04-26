package com.devscion.genai_pg_kmp.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual class DatabaseBuilder(private val context: Context) {
    actual fun get(): RoomDatabase.Builder<AppDatabase> {
        val applicationContext = context.applicationContext
//        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val dbFile = applicationContext.getDatabasePath(AppDatabase.DATABASE_NAME)
        return Room.databaseBuilder<AppDatabase>(
            context = applicationContext,
            factory = RoomDBConstructor::initialize,
            name = dbFile.absolutePath
        ).setDriver(BundledSQLiteDriver().apply {
            addExtension("sqlitevec", "sqlite3_vec_init")
        })
    }
}