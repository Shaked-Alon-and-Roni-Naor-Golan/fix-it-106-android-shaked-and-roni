package com.sr.fixit.data.users

data class UserDTO(
    val email: String = "",
    val name: String = "",
    val profile_picture: String = "",
    val city: String = "",
    val role: String = UserRole.RESIDENT,
    val id: String? = null
) {
    fun toUserModel(): UserModel {
        return UserModel(
            id = id ?: "",
            email = email,
            name = name,
            profile_picture = profile_picture,
            city = city,
            role = role.ifBlank { UserRole.RESIDENT }
        )
    }
}