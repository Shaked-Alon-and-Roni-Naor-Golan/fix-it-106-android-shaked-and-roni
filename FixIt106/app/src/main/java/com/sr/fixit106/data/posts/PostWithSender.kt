package com.sr.fixit106.data.posts

import androidx.room.Embedded
import androidx.room.Relation
import com.sr.fixit106.data.users.UserModel

data class PostWithSender(
    @Embedded val post: PostModel,
    @Relation(
        entity = UserModel::class,
        parentColumn = "userId",
        entityColumn = "id"
    ) val sender: UserModel
) {
}