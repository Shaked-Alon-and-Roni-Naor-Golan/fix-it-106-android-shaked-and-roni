package com.sr.fixit.data.comments

import androidx.room.Embedded
import androidx.room.Relation
import com.sr.fixit.data.users.UserModel

data class CommentWithSender(
    @Embedded val comment: CommentModel,
    @Relation(
        entity = UserModel::class,
        parentColumn = "userId",
        entityColumn = "id"
    ) val sender: UserModel
) {
}