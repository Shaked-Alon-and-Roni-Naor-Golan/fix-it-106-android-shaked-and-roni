package com.sr.fixit106.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sr.fixit106.data.comments.CommentDao
import com.sr.fixit106.data.comments.CommentModel
import com.sr.fixit106.data.posts.PostDao
import com.sr.fixit106.data.posts.PostModel
import com.sr.fixit106.data.users.UserModel
import com.sr.fixit106.data.users.UsersDao

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