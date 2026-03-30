package com.sr.fixit.data.comments

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.sr.fixit.data.posts.PostModel
import com.sr.fixit.data.users.UserModel
import java.util.UUID

@Entity(tableName = "comments",
    foreignKeys = [ForeignKey(
        entity = PostModel::class,
        parentColumns = ["id"],
        childColumns = ["postId"]
    ),
        ForeignKey(
            entity = UserModel::class,
            parentColumns = ["id"],  // Ensure this matches UserModel's primary key
            childColumns = ["userId"], // Ensure this matches CommentModel's foreign key
            onDelete = ForeignKey.CASCADE
        )])
data class CommentModel(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val postId: String,
    val userId: String,
    val content: String,
    val timestamp: Long
) {
    companion object {
        @JvmStatic
        fun fromMap(map: Map<String, Any>): CommentModel {
            return CommentModel(
                id = map["id"] as String,
                content = map["content"] as String,
                postId = map["postId"] as String,
                userId = map["userId"] as String,
                timestamp = (map["timestamp"] as Number).toLong()
            )
        }
    }
}
