package com.sr.fixit.data.comments

import androidx.lifecycle.LiveData
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sr.fixit.room.DatabaseHolder
import com.sr.fixit.data.users.UsersRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CommentsRepository {

    companion object {
        const val REP_USER_ID = "rep_106"
    }

    private val usersRepository = UsersRepository()
    private val commentDao = DatabaseHolder.getDatabase().commentDao()
    private val firestoreHandle = Firebase.firestore.collection("comments")

    fun getAllComments(): LiveData<List<CommentWithSender>> {
        return commentDao.getAllComments()
    }

    fun getCommentsWithSenderLiveByPostId(postId: String): LiveData<List<CommentWithSender>> {
        return commentDao.getCommentsWithSenderLiveByPostId(postId)
    }

    fun getCommentsLiveByPostId(postId: String): LiveData<List<CommentModel>> {
        return commentDao.getCommentsLiveByPostId(postId)
    }

    suspend fun getLatestRepTimestamp(postId: String): Long? = withContext(Dispatchers.IO) {
        commentDao.getLatestTimestampForRepresentative(postId)
    }

    suspend fun add(comment: CommentModel) = withContext(Dispatchers.IO) {
        usersRepository.cacheUserIfNotExisting(comment.userId)

        firestoreHandle.document(comment.id).set(comment).await()
        commentDao.add(comment)
    }

    suspend fun delete(comment: CommentModel) = withContext(Dispatchers.IO) {
        firestoreHandle.document(comment.id).delete().await()
        commentDao.delete(comment)
    }

    suspend fun deleteCommentsByPostId(postId: String) = withContext(Dispatchers.IO) {
        val comments = commentDao.getCommentsByPostId(postId)
        comments.forEach { comment ->
            firestoreHandle.document(comment.id).delete().await()
        }
        commentDao.deleteByPostId(postId)
    }

    suspend fun loadCommentsFromRemoteSource() = withContext(Dispatchers.IO) {
        val comments = firestoreHandle.orderBy("id")
            .get().await()
            .toObjects(CommentDTO::class.java)
            .map { it.toCommentModel() }

        if (comments.isNotEmpty()) {
            usersRepository.cacheUsersIfNotExisting(comments.map { it.userId })
            commentDao.upsertAll(*comments.toTypedArray())
        }
    }

    suspend fun loadCommentsForPost(postId: String) = withContext(Dispatchers.IO) {
        try {
            val query = firestoreHandle
                .whereEqualTo("postId", postId)
                .orderBy("timestamp")

            val comments = query.get().await()
                .toObjects(CommentDTO::class.java)
                .map { it.toCommentModel() }

            if (comments.isNotEmpty()) {
                usersRepository.cacheUsersIfNotExisting(comments.map { it.userId })
                commentDao.upsertAll(*comments.toTypedArray())
            }
        } catch (_: Exception) {
            val comments = firestoreHandle
                .whereEqualTo("postId", postId)
                .get().await()
                .toObjects(CommentDTO::class.java)
                .map { it.toCommentModel() }

            if (comments.isNotEmpty()) {
                usersRepository.cacheUsersIfNotExisting(comments.map { it.userId })
                commentDao.upsertAll(*comments.toTypedArray())
            }
        }
    }

    fun deleteAll() {
        commentDao.deleteAll()
    }
}