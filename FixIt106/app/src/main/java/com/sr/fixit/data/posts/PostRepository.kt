package com.sr.fixit.data.posts

import androidx.lifecycle.LiveData
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sr.fixit.data.comments.CommentsRepository
import com.sr.fixit.data.users.UserModel
import com.sr.fixit.data.users.UserRole
import com.sr.fixit.data.users.UsersRepository
import com.sr.fixit.room.DatabaseHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PostsRepository {
    private val postDao = DatabaseHolder.getDatabase().postDao()
    private val usersRepository = UsersRepository()
    private val commentsRepository = CommentsRepository()
    private val firestoreHandle = Firebase.firestore.collection("posts")

    private var pagedFeedLastVisible: DocumentSnapshot? = null
    private var pagedFeedHasMore: Boolean = true
    private var pagedFeedQueryKey: String? = null

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
        val now = System.currentTimeMillis()
        val normalized = post.copy(
            lastActivityTimestamp = if (post.lastActivityTimestamp > 0) {
                post.lastActivityTimestamp
            } else {
                now
            }
        )

        firestoreHandle.document(normalized.id).set(normalized).await()
        postDao.add(normalized)
    }

    suspend fun edit(post: PostModel) = withContext(Dispatchers.IO) {
        val updated = post.copy(lastActivityTimestamp = System.currentTimeMillis())
        firestoreHandle.document(updated.id).set(updated).await()
        postDao.upsertAll(updated)
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
        val now = System.currentTimeMillis()
        firestoreHandle.document(postId).update(
            mapOf(
                "status" to status,
                "lastActivityTimestamp" to now
            )
        ).await()
        postDao.updateStatus(postId, status, now)
    }

    suspend fun touchPostActivity(postId: String, activityTs: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            firestoreHandle.document(postId)
                .update("lastActivityTimestamp", activityTs)
                .await()
            postDao.updateLastActivity(postId, activityTs)
        }

    suspend fun loadPostsFromRemoteSource(limit: Int, offset: Int) =
        withContext(Dispatchers.IO) {
            var query: Query = firestoreHandle
                .orderBy("lastActivityTimestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            if (offset > 0) {
                val offsetSnapshot = firestoreHandle
                    .orderBy("lastActivityTimestamp", Query.Direction.DESCENDING)
                    .limit(offset.toLong())
                    .get()
                    .await()

                val lastOffsetDoc = offsetSnapshot.documents.lastOrNull()
                if (lastOffsetDoc != null) {
                    query = query.startAfter(lastOffsetDoc)
                }
            }

            val rawPosts = query.get()
                .await()
                .toObjects(PostDTO::class.java)
                .map { normalizeLoadedPost(it.toPostModel()) }

            val postsFromRoom = postDao.getAllPostsOnce()
            val firestorePostIds = rawPosts.map { it.id }.toSet()
            val postsToDelete = postsFromRoom.filter { !firestorePostIds.contains(it.post.id) }

            postsToDelete.forEach { postWithSender ->
                commentsRepository.deleteCommentsByPostId(postWithSender.post.id)
                postDao.delete(postWithSender.post)
            }

            if (rawPosts.isNotEmpty()) {
                usersRepository.cacheUsersIfNotExisting(rawPosts.map { it.userId })
                postDao.upsertAll(*rawPosts.toTypedArray())
            } else if (offset == 0) {
                commentsRepository.deleteAll()
                postDao.deleteAll()
            }
        }

    suspend fun resetFeedPagination(city: String?, status: String?) = withContext(Dispatchers.IO) {
        pagedFeedLastVisible = null
        pagedFeedHasMore = true
        pagedFeedQueryKey = buildFeedQueryKey(city, status)
    }

    suspend fun loadNextFeedPage(
        city: String?,
        status: String?,
        pageSize: Int
    ): List<PostWithSender> = withContext(Dispatchers.IO) {
        val queryKey = buildFeedQueryKey(city, status)
        if (pagedFeedQueryKey != queryKey) {
            pagedFeedLastVisible = null
            pagedFeedHasMore = true
            pagedFeedQueryKey = queryKey
        }

        if (!pagedFeedHasMore) {
            return@withContext emptyList()
        }

        val matchedPosts = mutableListOf<PostModel>()
        val batchSize = maxOf(pageSize, 20)

        while (matchedPosts.size < pageSize && pagedFeedHasMore) {
            var query: Query = firestoreHandle
                .orderBy("lastActivityTimestamp", Query.Direction.DESCENDING)
                .limit(batchSize.toLong())

            pagedFeedLastVisible?.let { last ->
                query = query.startAfter(last)
            }

            val snapshot = query.get().await()
            val docs = snapshot.documents

            if (docs.isEmpty()) {
                pagedFeedHasMore = false
                break
            }

            pagedFeedLastVisible = docs.last()
            if (docs.size < batchSize) {
                pagedFeedHasMore = false
            }

            val rawPosts = docs
                .mapNotNull { it.toObject(PostDTO::class.java)?.toPostModel() }
                .map { normalizeLoadedPost(it) }

            if (rawPosts.isNotEmpty()) {
                usersRepository.cacheUsersIfNotExisting(rawPosts.map { it.userId })
                postDao.upsertAll(*rawPosts.toTypedArray())
            }

            val filteredPosts = rawPosts.filter { post ->
                matchesCity(post, city) && matchesStatus(post, status)
            }

            filteredPosts.forEach { post ->
                if (matchedPosts.size < pageSize) {
                    matchedPosts.add(post)
                }
            }
        }

        val result = matchedPosts.map { post ->
            val sender = usersRepository.getUserByUid(post.userId)
                ?: UserModel(
                    id = post.userId,
                    name = "Resident",
                    email = "",
                    profile_picture = "",
                    city = "",
                    role = UserRole.RESIDENT
                )

            PostWithSender(
                post = post,
                sender = sender
            )
        }

        return@withContext result
    }

    fun hasMoreFeedPages(): Boolean = pagedFeedHasMore

    private fun buildFeedQueryKey(city: String?, status: String?): String {
        val normalizedCity = city?.trim().orEmpty()
        val normalizedStatus = status?.trim().orEmpty()
        return "$normalizedCity|$normalizedStatus"
    }

    private fun matchesCity(post: PostModel, city: String?): Boolean {
        if (city.isNullOrBlank()) return true
        return post.city.trim().equals(city.trim(), ignoreCase = true)
    }

    private fun matchesStatus(post: PostModel, status: String?): Boolean {
        if (status.isNullOrBlank()) return true
        return post.status == status
    }

    private fun normalizeLoadedPost(post: PostModel): PostModel {
        val resolvedLastActivity = if (post.lastActivityTimestamp > 0) {
            post.lastActivityTimestamp
        } else {
            post.timestamp
        }

        return post.copy(lastActivityTimestamp = resolvedLastActivity)
    }
}