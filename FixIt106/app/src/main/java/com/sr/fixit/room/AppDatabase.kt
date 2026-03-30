package com.sr.fixit.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sr.fixit.data.comments.CommentDao
import com.sr.fixit.data.comments.CommentModel
import com.sr.fixit.data.posts.PostDao
import com.sr.fixit.data.posts.PostModel
import com.sr.fixit.data.users.UserModel
import com.sr.fixit.data.users.UsersDao

@Database(
    entities = [PostModel::class, UserModel::class, CommentModel::class],
    version = 16,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usersDao(): UsersDao
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
}