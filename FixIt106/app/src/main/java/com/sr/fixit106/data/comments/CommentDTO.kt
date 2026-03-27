package com.sr.fixit106.data.comments

data class CommentDTO(
    val id: String? = null,
    val postId: String = "",
    val userId: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toCommentModel(): CommentModel {
        return CommentModel(
            id = id ?: "",
            postId = postId,
            userId = userId,
            content = content,
            timestamp = timestamp
        )
    }
}
