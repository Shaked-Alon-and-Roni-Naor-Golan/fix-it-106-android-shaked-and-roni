package com.sr.fixit.data.posts

import androidx.room.Embedded
import androidx.room.Relation
import com.sr.fixit.data.users.UserModel

data class PostWithSender(
    @Embedded val post: PostModel,
    @Relation(
        entity = UserModel::class,
        parentColumn = "userId",
        entityColumn = "id"
    ) val sender: UserModel
) {
}