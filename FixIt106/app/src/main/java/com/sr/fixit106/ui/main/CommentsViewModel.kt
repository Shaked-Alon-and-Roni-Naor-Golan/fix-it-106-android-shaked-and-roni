package com.sr.fixit106.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sr.fixit106.data.comments.CommentModel
import com.sr.fixit106.data.comments.CommentWithSender
import com.sr.fixit106.data.comments.CommentsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CommentsViewModel() : ViewModel() {
    private val repository = CommentsRepository() // Adjusted repository for posts
    fun getAllComments(): LiveData<List<CommentWithSender>> {
        viewModelScope.launch(Dispatchers.IO) {
            repository.loadCommentsFromRemoteSource() // Fetch posts from remote source
        }
        return repository.getAllComments()
    }

    fun addComment(comment: CommentModel) = viewModelScope.launch {
        repository.add(comment)
    }
}
