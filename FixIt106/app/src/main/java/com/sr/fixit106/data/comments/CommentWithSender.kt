package com.sr.fixit106.data.comments

import androidx.room.Embedded
import androidx.room.Relation
import com.sr.fixit106.data.users.UserModel

data class CommentWithSender(
    @Embedded val comment: CommentModel,
    @Relation(
        entity = UserModel::class,
        parentColumn = "userId",
        entityColumn = "id"
    ) val sender: UserModel
) {
}