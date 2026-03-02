package com.devscion.genai_pg_kmp.data.database

import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

expect class DatabaseBuilder {
    fun get(): RoomDatabase.Builder<AppDatabase>
}

@Suppress("KotlinNoActualForExpect")
expect object RoomDBConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}