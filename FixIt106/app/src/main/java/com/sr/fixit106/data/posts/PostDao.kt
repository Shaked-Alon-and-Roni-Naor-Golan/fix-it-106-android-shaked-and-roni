package com.sr.fixit106.data.posts

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PostDao {

    @Transaction
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): LiveData<List<PostWithSender>>

    @Transaction
    @Query("SELECT * FROM posts WHERE status = :status ORDER BY timestamp DESC")
    fun getPostsByStatus(status: String): LiveData<List<PostWithSender>>

    @Transaction
    @Query("SELECT * FROM posts WHERE LOWER(city) = LOWER(:city) ORDER BY timestamp DESC")
    fun getPostsByCity(city: String): LiveData<List<PostWithSender>>

    @Transaction
    @Query("SELECT * FROM posts WHERE LOWER(city) = LOWER(:city) AND status = :status ORDER BY timestamp DESC")
    fun getPostsByCityAndStatus(city: String, status: String): LiveData<List<PostWithSender>>

    @Transaction
    @Query("SELECT * FROM posts")
    suspend fun getAllPostsOnce(): List<PostWithSender>

    @Transaction
    @Query("SELECT * FROM posts WHERE id = :id")
    fun getById(id: String): LiveData<PostWithSender?>

    @Transaction
    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getDataById(id: String): PostWithSender?

    @Transaction
    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY timestamp DESC")
    fun getPostsByUserId(userId: String): LiveData<List<PostWithSender>>

    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getPostsByUserIdOnce(userId: String): List<PostModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(vararg post: PostModel)

    @Update
    fun update(post: PostModel)

    @Delete
    fun delete(post: PostModel)

    @Query("DELETE FROM posts WHERE id = :id")
    fun deleteById(id: String)

    @Upsert
    fun upsertAll(vararg post: PostModel)

    @Query("DELETE FROM posts")
    fun deleteAll()

    @Query("UPDATE posts SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}