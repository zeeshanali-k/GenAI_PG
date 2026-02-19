package com.devscion.genai_pg_kmp.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.devscion.genai_pg_kmp.data.database.converters.AttachmentListConverter
import com.devscion.genai_pg_kmp.data.database.dao.ChatDao
import com.devscion.genai_pg_kmp.data.database.dao.MessageDao
import com.devscion.genai_pg_kmp.data.database.entity.ChatEntity
import com.devscion.genai_pg_kmp.data.database.entity.MessageEntity

@Database(
    entities = [ChatEntity::class, MessageEntity::class], version = 1,
    exportSchema = true,
)
@ConstructedBy(RoomDBConstructor::class)
@TypeConverters(AttachmentListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao

    companion object {
        const val DATABASE_NAME = "genai_pg_db"
    }
}
