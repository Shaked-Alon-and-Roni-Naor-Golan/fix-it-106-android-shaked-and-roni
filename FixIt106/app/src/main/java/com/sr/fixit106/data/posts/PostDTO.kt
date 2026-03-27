package com.sr.fixit106.data.posts

import kotlin.text.ifBlank

data class PostDTO(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val userId: String = "",
    val locationLng: Double = 0.0,
    val locationLat: Double = 0.0,
    val image: String? = null,
    val tags: List<String>? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = PostStatus.NEW,
    val city: String = ""
) {
    fun toPostModel(): PostModel {
        return PostModel(
            id = id ?: "",
            title = title,
            description = description,
            userId = userId,
            locationLng = locationLng,
            locationLat = locationLat,
            image = image,
            timestamp = timestamp,
            tags = tags,
            status = status.ifBlank { PostStatus.NEW },
            city = city
        )
    }
}