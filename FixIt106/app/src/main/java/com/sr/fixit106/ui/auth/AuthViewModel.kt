package com.sr.fixit106.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sr.fixit106.data.users.UserModel
import com.sr.fixit106.data.users.UserRole
import com.sr.fixit106.data.users.UsersRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val usersRepository = UsersRepository()

    fun register(
        onFinishUi: () -> Unit,
        userImage: String,
        role: String = UserRole.RESIDENT,
        city: String = ""
    ) {
        viewModelScope.launch {
            usersRepository.upsertUser(
                UserModel.fromFirebaseAuth(
                    userImage = userImage,
                    city = city,
                    role = role
                )
            )
            onFinishUi()
        }
    }

    suspend fun getUserByUid(uid: String): UserModel? {
        return usersRepository.getUserByUid(uid)
    }
}