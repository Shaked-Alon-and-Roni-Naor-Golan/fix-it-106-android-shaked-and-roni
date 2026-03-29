package com.sr.fixit.data.posts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.sr.fixit.data.StringListConverters
import com.sr.fixit.data.users.UserModel
import java.util.UUID

@Entity(
    tableName = "posts",
    foreignKeys = [ForeignKey(
        entity = UserModel::class,
        parentColumns = ["id"],
        childColumns = ["userId"]
    )]
)
@TypeConverters(StringListConverters::class)
data class PostModel(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "userId") val userId: String,
    @ColumnInfo(name = "locationLng") val locationLng: Double,
    @ColumnInfo(name = "locationLat") val locationLat: Double,
    @ColumnInfo(name = "image") val image: String? = null,
    @ColumnInfo(name = "tags") val tags: List<String>? = null,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "lastActivityTimestamp") val lastActivityTimestamp: Long = timestamp,
    @ColumnInfo(name = "status") val status: String = PostStatus.NEW,
    @ColumnInfo(name = "city") val city: String = ""
) {
    fun toPostDto(): PostDTO {
        return PostDTO(
            id = id,
            title = title,
            description = description,
            userId = userId,
            locationLng = locationLng,
            locationLat = locationLat,
            image = image,
            timestamp = timestamp,
            lastActivityTimestamp = lastActivityTimestamp,
            tags = tags,
            status = status,
            city = city
        )
    }
}