package com.sr.fixit106.ui.main

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.sr.fixit106.BuildConfig
import com.sr.fixit106.data.comments.CommentsRepository
import com.sr.fixit106.data.posts.PostModel
import com.sr.fixit106.data.posts.PostWithSender
import com.sr.fixit106.data.posts.PostsRepository
import com.sr.fixit106.data.users.UserModel
import com.sr.fixit106.data.users.UserRole
import com.sr.fixit106.data.users.UsersRepository
import com.sr.fixit106.utils.GeminiApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.distinct
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.getOrElse
import kotlin.runCatching
import kotlin.text.ifBlank
import kotlin.text.isBlank
import kotlin.text.isNotBlank
import kotlin.text.isNullOrBlank
import kotlin.text.orEmpty
import kotlin.text.removePrefix
import kotlin.text.replace
import kotlin.text.trim

data class PostsUiState(val reviewId: String = "")

class PostsViewModel : ViewModel() {
    companion object {
        private const val FEED_PAGE_SIZE = 20
    }

    private val repository = PostsRepository()
    private val usersRepository = UsersRepository()
    private val commentsRepository = CommentsRepository()

    private val feedInitialLoadingLiveData = MutableLiveData(false)
    private val feedLoadingMoreLiveData = MutableLiveData(false)
    private val feedHasMorePagesLiveData = MutableLiveData(true)

    private val currentFeedStatusLiveData = MutableLiveData<String?>(null)

    private var currentFeedStatus: String? = null
    private var currentFeedCity: String? = null
    private var feedInitialized = false

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.loadPostsFromRemoteSource(50, 0)
        }
    }

    fun getAllPosts(): LiveData<List<PostWithSender>> {
        viewModelScope.launch(Dispatchers.IO) {
            repository.loadPostsFromRemoteSource(50, 0)
        }

        return getCurrentUserLive().switchMap { user ->
            if (user?.city.isNullOrBlank()) {
                repository.getAllPosts()
            } else {
                repository.getPostsByCity(user!!.city, null)
            }
        }
    }

    fun getPosts(status: String?): LiveData<List<PostWithSender>> {
        viewModelScope.launch(Dispatchers.IO) {
            repository.loadPostsFromRemoteSource(50, 0)
        }

        return getCurrentUserLive().switchMap { user ->
            if (user?.city.isNullOrBlank()) {
                repository.getPosts(status)
            } else {
                repository.getPostsByCity(user!!.city, status)
            }
        }
    }

    fun getPostsByUserId(userId: String): LiveData<List<PostWithSender>> {
        viewModelScope.launch(Dispatchers.IO) {
            repository.loadPostsFromRemoteSource(50, 0)
        }
        return repository.getPostsByUserId(userId)
    }

    fun getPostByIdLive(postId: String): LiveData<PostWithSender?> {
        return repository.getPostByIdLive(postId)
    }

    fun getUserById(id: String): LiveData<UserModel?> {
        viewModelScope.launch(Dispatchers.IO) {
            usersRepository.cacheUserIfNotExisting(id)
        }
        return usersRepository.observeUserByUid(id)
    }

    fun getCurrentUserLive(): LiveData<UserModel?> {
        val uid = getCurrentUserIdOrEmpty()
        return if (uid.isBlank()) {
            MutableLiveData(null)
        } else {
            getUserById(uid)
        }
    }

    suspend fun getCurrentUserOnce(): UserModel? {
        val uid = getCurrentUserIdOrEmpty()
        if (uid.isBlank()) return null

        return withContext(Dispatchers.IO) {
            usersRepository.getUserByUid(uid)
        }
    }

    suspend fun getCurrentUserCityOrEmpty(): String {
        return getCurrentUserOnce()?.city.orEmpty()
    }

    fun doUserExist(fireBaseUser: FirebaseUser): LiveData<UserModel?> {
        val userLiveData = MutableLiveData<UserModel?>()
        viewModelScope.launch {
            val user = withContext(Dispatchers.IO) {
                usersRepository.getUserByEmail(fireBaseUser.email!!)
            }
            userLiveData.postValue(user)
        }
        return userLiveData
    }

    fun updateUser(
        user: UserModel,
        onComplete: ((success: Boolean, error: Throwable?) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                usersRepository.upsertUser(user)
                onComplete?.invoke(true, null)
            } catch (t: Throwable) {
                Log.e("PostsViewModel", "Failed to update user", t)
                onComplete?.invoke(false, t)
            }
        }
    }

    fun getCurrentUserIdOrEmpty(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }

    suspend fun isCurrentUserRepresentative(): Boolean {
        val uid = getCurrentUserIdOrEmpty()
        if (uid.isBlank()) return false

        val user = withContext(Dispatchers.IO) {
            usersRepository.getUserByUid(uid)
        }
        return UserRole.isRepresentative(user?.role)
    }

    suspend fun generateTagsForPost(title: String, description: String): List<String> {
        val apiKey = BuildConfig.API_KEY
            .replace("\"", "")
            .trim()

        Log.d("PostsViewModel", "Gemini API key present: ${apiKey.isNotBlank()}")

        if (apiKey.isBlank()) {
            Log.e("PostsViewModel", "Gemini API key is blank")
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                val tags = GeminiApiClient(apiKey)
                    .generateTags(title, description)
                    .map { it.removePrefix("#").trim() }
                    .filter { it.isNotBlank() }
                    .distinct()

                Log.d("PostsViewModel", "Generated tags: $tags")
                tags
            }.getOrElse { error ->
                Log.e("PostsViewModel", "Failed to generate tags", error)
                emptyList()
            }
        }
    }

    fun addPost(
        post: PostModel,
        onComplete: ((success: Boolean, error: Throwable?) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                repository.add(post)
                repository.loadPostsFromRemoteSource(50, 0)
                refreshFeedIfNeeded()
                onComplete?.invoke(true, null)
            } catch (t: Throwable) {
                Log.e("PostsViewModel", "Failed to add post", t)
                onComplete?.invoke(false, t)
            }
        }
    }

    fun editPost(
        post: PostModel,
        onComplete: ((success: Boolean, error: Throwable?) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                repository.edit(post)
                repository.loadPostsFromRemoteSource(50, 0)
                refreshFeedIfNeeded()
                onComplete?.invoke(true, null)
            } catch (t: Throwable) {
                Log.e("PostsViewModel", "Failed to edit post", t)
                onComplete?.invoke(false, t)
            }
        }
    }

    fun invalidatePosts() {
        viewModelScope.launch {
            repository.loadPostsFromRemoteSource(50, 0)
            refreshFeedIfNeeded()
        }
    }

    fun deletePostById(postId: String, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.Main) {
            commentsRepository.deleteCommentsByPostId(postId)
            repository.deleteById(postId)
            refreshFeedIfNeeded()
            onComplete()
        }
    }

    fun isPostValid(title: String, image: String?, description: String): Boolean {
        return title.isNotBlank() && !image.isNullOrBlank() && description.isNotBlank()
    }

    fun updatePostStatus(postId: String, status: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.Main) {
            repository.updateStatus(postId, status)
            repository.loadPostsFromRemoteSource(50, 0)
            refreshFeedIfNeeded()
            onComplete?.invoke()
        }
    }

    fun observeFeedPosts(): LiveData<List<PostWithSender>> {
        val result = MediatorLiveData<List<PostWithSender>>()

        var postsSource: LiveData<List<PostWithSender>>? = null
        var userSource: LiveData<UserModel?>? = null

        fun rebuildSource(status: String?) {
            val oldUserSource = userSource
            if (oldUserSource != null) {
                result.removeSource(oldUserSource)
            }

            val newUserSource = getCurrentUserLive()
            userSource = newUserSource

            result.addSource(newUserSource) { user ->
                val oldPostsSource = postsSource
                if (oldPostsSource != null) {
                    result.removeSource(oldPostsSource)
                }

                val newPostsSource = if (user?.city.isNullOrBlank()) {
                    repository.getPosts(status)
                } else {
                    repository.getPostsByCity(user!!.city, status)
                }

                postsSource = newPostsSource
                result.addSource(newPostsSource) { posts ->
                    result.value = posts
                }
            }
        }

        result.addSource(currentFeedStatusLiveData) { status ->
            rebuildSource(status)
        }

        rebuildSource(currentFeedStatusLiveData.value)

        return result
    }

    fun observeFeedInitialLoading(): LiveData<Boolean> = feedInitialLoadingLiveData
    fun observeFeedLoadingMore(): LiveData<Boolean> = feedLoadingMoreLiveData
    fun observeFeedHasMorePages(): LiveData<Boolean> = feedHasMorePagesLiveData

    fun resetAndLoadFeed(status: String?) {
        viewModelScope.launch(Dispatchers.Main) {
            val city = getCurrentUserCityOrEmpty().ifBlank { null }

            currentFeedStatus = status
            currentFeedCity = city
            feedInitialized = true
            currentFeedStatusLiveData.value = status

            feedInitialLoadingLiveData.value = true
            feedLoadingMoreLiveData.value = false
            feedHasMorePagesLiveData.value = true

            try {
                repository.resetFeedPagination(city, status)
                repository.loadNextFeedPage(
                    city = city,
                    status = status,
                    pageSize = FEED_PAGE_SIZE
                )
                feedHasMorePagesLiveData.value = repository.hasMoreFeedPages()
            } catch (t: Throwable) {
                Log.e("PostsViewModel", "Failed to load first feed page", t)
                feedHasMorePagesLiveData.value = false
            } finally {
                feedInitialLoadingLiveData.value = false
            }
        }
    }

    fun refreshFeed() {
        resetAndLoadFeed(currentFeedStatus)
    }

    fun loadNextFeedPage() {
        val isInitialLoading = feedInitialLoadingLiveData.value == true
        val isLoadingMore = feedLoadingMoreLiveData.value == true
        val hasMore = feedHasMorePagesLiveData.value == true

        if (!feedInitialized || isInitialLoading || isLoadingMore || !hasMore) {
            return
        }

        viewModelScope.launch(Dispatchers.Main) {
            feedLoadingMoreLiveData.value = true
            try {
                repository.loadNextFeedPage(
                    city = currentFeedCity,
                    status = currentFeedStatus,
                    pageSize = FEED_PAGE_SIZE
                )
                feedHasMorePagesLiveData.value = repository.hasMoreFeedPages()
            } catch (t: Throwable) {
                Log.e("PostsViewModel", "Failed to load next feed page", t)
            } finally {
                feedLoadingMoreLiveData.value = false
            }
        }
    }

    private suspend fun refreshFeedIfNeeded() {
        if (!feedInitialized) return
        val city = getCurrentUserCityOrEmpty().ifBlank { null }
        currentFeedCity = city
        repository.resetFeedPagination(city, currentFeedStatus)
        repository.loadNextFeedPage(
            city = city,
            status = currentFeedStatus,
            pageSize = FEED_PAGE_SIZE
        )
        feedHasMorePagesLiveData.postValue(repository.hasMoreFeedPages())
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences("fixit106_notif", Context.MODE_PRIVATE)

    private fun keyLastRead(postId: String) = "last_read_$postId"

    suspend fun markPostChatAsRead(context: Context, postId: String) {
        withContext(Dispatchers.IO) {
            prefs(context).edit()
                .putLong(keyLastRead(postId), System.currentTimeMillis())
                .apply()
        }
    }

    suspend fun getUnreadNotificationsCountForCurrentUser(context: Context): Int {
        val uid = getCurrentUserIdOrEmpty()
        if (uid.isBlank()) return 0

        return withContext(Dispatchers.IO) {
            repository.loadPostsFromRemoteSource(50, 0)

            val myPosts = repository.getMyPostsOnce(uid)
            var count = 0

            for (p in myPosts) {
                val lastRead = prefs(context).getLong(keyLastRead(p.id), 0L)
                val latestRep = commentsRepository.getLatestRepTimestamp(p.id)
                if (latestRep != null && latestRep > lastRead) count++
            }
            count
        }
    }
}