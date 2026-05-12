package com.weblog.android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [QSO::class], version = 1, exportSchema = false)
abstract class QSODatabase : RoomDatabase() {
    abstract fun qsoDao(): QSODao

    companion object {
        @Volatile private var INSTANCE: QSODatabase? = null

        fun getInstance(context: Context): QSODatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    QSODatabase::class.java,
                    "weblog.db"
                ).build().also { INSTANCE = it }
            }
    }
}
