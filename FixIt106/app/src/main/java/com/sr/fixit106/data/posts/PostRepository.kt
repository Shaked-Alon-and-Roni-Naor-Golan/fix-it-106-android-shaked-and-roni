package com.sr.fixit106.data.posts

import androidx.lifecycle.LiveData
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sr.fixit106.data.comments.CommentsRepository
import com.sr.fixit106.data.users.UsersRepository
import com.sr.fixit106.room.DatabaseHolder
import com.sr.fixit106.utils.AppContextProvider
import com.sr.fixit106.utils.CityLocationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.collections.filter
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.toSet
import kotlin.collections.toTypedArray
import kotlin.jvm.java
import kotlin.runCatching
import kotlin.text.isBlank
import kotlin.text.isNotBlank
import kotlin.text.isNullOrBlank

class PostsRepository {
    private val postDao = DatabaseHolder.getDatabase().postDao()
    private val usersRepository = UsersRepository()
    private val commentsRepository = CommentsRepository()
    private val firestoreHandle = Firebase.firestore.collection("posts")

    fun getAllPosts(): LiveData<List<PostWithSender>> {
        return postDao.getAllPosts()
    }

    fun getPosts(status: String?): LiveData<List<PostWithSender>> {
        return if (status.isNullOrBlank()) {
            postDao.getAllPosts()
        } else {
            postDao.getPostsByStatus(status)
        }
    }

    fun getPostsByCity(city: String, status: String?): LiveData<List<PostWithSender>> {
        return if (status.isNullOrBlank()) {
            postDao.getPostsByCity(city)
        } else {
            postDao.getPostsByCityAndStatus(city, status)
        }
    }

    fun getPostsByUserId(userId: String): LiveData<List<PostWithSender>> {
        return postDao.getPostsByUserId(userId)
    }

    fun getPostByIdLive(id: String): LiveData<PostWithSender?> {
        return postDao.getById(id)
    }

    suspend fun getMyPostsOnce(userId: String): List<PostModel> = withContext(Dispatchers.IO) {
        postDao.getPostsByUserIdOnce(userId)
    }

    suspend fun add(post: PostModel) = withContext(Dispatchers.IO) {
        firestoreHandle.document(post.id).set(post).await()
        postDao.add(post)
    }

    suspend fun edit(post: PostModel) = withContext(Dispatchers.IO) {
        firestoreHandle.document(post.id).set(post).await()
        postDao.upsertAll(post)
    }

    suspend fun delete(post: PostModel) = withContext(Dispatchers.IO) {
        firestoreHandle.document(post.id).delete().await()
        postDao.delete(post)
    }

    suspend fun deleteById(id: String) = withContext(Dispatchers.IO) {
        firestoreHandle.document(id).delete().await()
        postDao.deleteById(id)
    }

    suspend fun updateStatus(postId: String, status: String) = withContext(Dispatchers.IO) {
        firestoreHandle.document(postId).update("status", status).await()
        postDao.updateStatus(postId, status)
    }

    suspend fun loadPostsFromRemoteSource(limit: Int, offset: Int) =
        withContext(Dispatchers.IO) {
            val rawPosts = firestoreHandle.orderBy("id")
                .startAt(offset)
                .limit(limit.toLong())
                .get()
                .await()
                .toObjects(PostDTO::class.java)
                .map { it.toPostModel() }

            val posts = rawPosts.map { post ->
                if (post.city.isBlank()) {
                    val resolvedCity = CityLocationUtils.getCityNameFromCoordinates(
                        AppContextProvider.appContext,
                        post.locationLat,
                        post.locationLng
                    )

                    if (resolvedCity.isNotBlank()) {
                        val enriched = post.copy(city = resolvedCity)
                        runCatching {
                            firestoreHandle.document(enriched.id).set(enriched).await()
                        }
                        enriched
                    } else {
                        post
                    }
                } else {
                    post
                }
            }

            val postsFromRoom = postDao.getAllPostsOnce()
            val firestorePostIds = posts.map { it.id }.toSet()
            val postsToDelete = postsFromRoom.filter { !firestorePostIds.contains(it.post.id) }

            postsToDelete.forEach { postWithSender ->
                commentsRepository.deleteCommentsByPostId(postWithSender.post.id)
                postDao.delete(postWithSender.post)
            }

            if (posts.isNotEmpty()) {
                usersRepository.cacheUsersIfNotExisting(posts.map { it.userId })
                postDao.upsertAll(*posts.toTypedArray())
            } else {
                commentsRepository.deleteAll()
                postDao.deleteAll()
            }
        }
}