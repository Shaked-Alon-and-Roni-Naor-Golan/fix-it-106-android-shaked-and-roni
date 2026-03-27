package com.sr.fixit106.room

import android.content.Context
import androidx.room.Room

object DatabaseHolder {
    @Volatile
    private var appDatabase: AppDatabase? = null

    fun initDatabase(context: Context) {
        appDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "fixit106-db").fallbackToDestructiveMigration()
            .build()
    }

    fun getDatabase(): AppDatabase {
        return this.appDatabase!!
    }
}