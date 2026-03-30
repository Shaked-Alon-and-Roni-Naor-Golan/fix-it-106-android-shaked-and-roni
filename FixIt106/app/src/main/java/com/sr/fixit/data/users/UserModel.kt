package com.sr.fixit.data.users

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.auth.FirebaseAuth

@Entity(tableName = "users")
data class UserModel(
    @PrimaryKey val id: String = "",
    val name: String,
    val email: String,
    val profile_picture: String,
    val city: String = "",
    val role: String = UserRole.RESIDENT
) {
    fun toUserDto(): UserDTO {
        return UserDTO(
            id = id,
            name = name,
            email = email,
            profile_picture = profile_picture,
            city = city,
            role = role
        )
    }

    companion object {
        fun fromFirebaseAuth(
            userImage: String,
            city: String = "",
            role: String = UserRole.RESIDENT
        ): UserModel {
            val user = FirebaseAuth.getInstance().currentUser

            return UserModel(
                id = user?.uid!!,
                email = user.email ?: "",
                name = user.displayName ?: "Resident",
                profile_picture = userImage,
                city = city,
                role = role
            )
        }
    }
}