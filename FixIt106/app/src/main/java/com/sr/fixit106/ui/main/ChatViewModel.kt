package com.sr.fixit106.ui.main.fragments.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sr.fixit106.data.comments.CommentModel
import com.sr.fixit106.data.comments.CommentWithSender
import com.sr.fixit106.data.comments.CommentsRepository
import com.sr.fixit106.data.posts.PostStatus
import com.sr.fixit106.data.posts.PostsRepository
import com.sr.fixit106.data.users.UserModel
import com.sr.fixit106.data.users.UserRole
import com.sr.fixit106.data.users.UsersRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    companion object {
        const val REP_USER_ID = "rep_106"
        const val SYSTEM_TAKEN_CARE_MESSAGE = "This issue is taken care of by a 106 representative"
    }

    private val commentsRepo = CommentsRepository()
    private val usersRepo = UsersRepository()
    private val postsRepo = PostsRepository()

    fun observeComments(postId: String): LiveData<List<CommentWithSender>> {
        viewModelScope.launch(Dispatchers.IO) {
            ensureRepSystemUserExists()
            ensureCurrentUserCached()
            commentsRepo.loadCommentsForPost(postId)
        }
        return commentsRepo.getCommentsWithSenderLiveByPostId(postId)
    }

    fun sendUserMessage(postId: String, text: String) {
        val authUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = authUser.uid

        viewModelScope.launch(Dispatchers.IO) {
            ensureCurrentUserCached()

            val now = System.currentTimeMillis()
            val comment = CommentModel(
                postId = postId,
                userId = uid,
                content = text,
                timestamp = now
            )
            commentsRepo.add(comment)
            postsRepo.touchPostActivity(postId, now)
        }
    }

    fun sendRepMessage(postId: String, text: String) {
        val authUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = authUser.uid

        viewModelScope.launch(Dispatchers.IO) {
            ensureCurrentUserCached()

            val now = System.currentTimeMillis()
            val comment = CommentModel(
                postId = postId,
                userId = uid,
                content = text,
                timestamp = now
            )

            commentsRepo.add(comment)
            postsRepo.updateStatus(postId, PostStatus.IN_PROGRESS)
        }
    }

    fun onRepresentativeOpenedNewIssue(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val authUser = FirebaseAuth.getInstance().currentUser ?: return@launch
            val currentUser = usersRepo.getUserByUid(authUser.uid) ?: return@launch

            if (!UserRole.isRepresentative(currentUser.role)) return@launch

            ensureRepSystemUserExists()

            val now = System.currentTimeMillis()
            val systemComment = CommentModel(
                postId = postId,
                userId = REP_USER_ID,
                content = SYSTEM_TAKEN_CARE_MESSAGE,
                timestamp = now
            )

            commentsRepo.add(systemComment)
            postsRepo.updateStatus(postId, PostStatus.IN_PROGRESS)
        }
    }

    private suspend fun ensureRepSystemUserExists() {
        val rep = UserModel(
            id = REP_USER_ID,
            name = "106 Support",
            email = "support@106.local",
            profile_picture = "",
            city = "",
            role = UserRole.REPRESENTATIVE
        )
        usersRepo.upsertUser(rep)
    }

    private suspend fun ensureCurrentUserCached() {
        val authUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = authUser.uid

        val existingUser = usersRepo.getUserByUid(uid)
        if (existingUser != null) {
            usersRepo.upsertUser(
                existingUser.copy(
                    name = authUser.displayName?.takeIf { it.isNotBlank() } ?: existingUser.name,
                    email = authUser.email ?: existingUser.email
                )
            )
            return
        }

        val displayName = authUser.displayName?.takeIf { it.isNotBlank() } ?: "Resident"
        val email = authUser.email ?: ""

        val user = UserModel(
            id = uid,
            name = displayName,
            email = email,
            profile_picture = "",
            city = "",
            role = UserRole.RESIDENT
        )
        usersRepo.upsertUser(user)
    }
}