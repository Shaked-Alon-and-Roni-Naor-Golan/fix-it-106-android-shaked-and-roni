package com.sr.fixit106.data.comments

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.sr.fixit106.data.users.UserRole

@Dao
interface CommentDao {

    @Transaction
    @Query("SELECT * FROM comments ORDER BY timestamp ASC")
    fun getAllComments(): LiveData<List<CommentWithSender>>

    @Transaction
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsWithSenderLiveByPostId(postId: String): LiveData<List<CommentWithSender>>

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsLiveByPostId(postId: String): LiveData<List<CommentModel>>

    @Query("SELECT * FROM comments WHERE postId = :postId")
    suspend fun getCommentsByPostId(postId: String): List<CommentModel>

    @Query(
        """
        SELECT MAX(c.timestamp)
        FROM comments c
        INNER JOIN users u ON u.id = c.userId
        WHERE c.postId = :postId AND u.role = :representativeRole
        """
    )
    suspend fun getLatestTimestampForRepresentative(
        postId: String,
        representativeRole: String = UserRole.REPRESENTATIVE
    ): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(vararg comment: CommentModel)

    @Delete
    fun delete(comment: CommentModel)

    @Query("DELETE FROM comments WHERE postId = :postId")
    suspend fun deleteByPostId(postId: String)

    @Upsert
    fun upsertAll(vararg comments: CommentModel)

    @Query("DELETE FROM comments")
    fun deleteAll()
}