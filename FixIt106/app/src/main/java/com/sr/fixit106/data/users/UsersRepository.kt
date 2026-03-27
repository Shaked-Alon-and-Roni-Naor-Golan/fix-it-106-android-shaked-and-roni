package com.sr.fixit106.data.users

import androidx.lifecycle.LiveData
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sr.fixit106.room.DatabaseHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UsersRepository {
    private val usersDao = DatabaseHolder.getDatabase().usersDao()
    private val firestoreHandle = Firebase.firestore.collection("users")

    suspend fun upsertUser(user: UserModel) = withContext(Dispatchers.IO) {
        firestoreHandle.document(user.id).set(user).await()
        usersDao.upsertAll(user)
    }

    fun observeUserByUid(uid: String): LiveData<UserModel?> {
        return usersDao.observeUserByUid(uid)
    }

    suspend fun deleteById(id: String) = withContext(Dispatchers.IO) {
        usersDao.deleteByUid(id)
    }

    suspend fun cacheUserIfNotExisting(uid: String) = withContext(Dispatchers.IO) {
        val cachedResult = usersDao.getUserByUid(uid)
        if (cachedResult == null) {
            getUserFromRemoteSource(uid)
        }
    }

    suspend fun cacheUsersIfNotExisting(uids: List<String>) = withContext(Dispatchers.IO) {
        val existingUserIds = usersDao.getExistingUserIds(uids)
        val nonExistingUserIds = uids.filter { !existingUserIds.contains(it) }
        getUsersFromRemoteSource(nonExistingUserIds)
    }

    suspend fun getUserByUid(uid: String): UserModel? = withContext(Dispatchers.IO) {
        val cachedResult = usersDao.getUserByUid(uid)
        if (cachedResult != null) return@withContext cachedResult
        return@withContext getUserFromRemoteSource(uid)
    }

    suspend fun getUserByEmail(email: String): UserModel? = withContext(Dispatchers.IO) {
        val cachedResult = usersDao.getUserByEmail(email)
        if (cachedResult != null) return@withContext cachedResult
        return@withContext getUserFromRemoteSourceByEmail(email)
    }

    private suspend fun getUserFromRemoteSource(uid: String): UserModel? =
        withContext(Dispatchers.IO) {
            val user = firestoreHandle.document(uid)
                .get().await()
                .toObject(UserDTO::class.java)
                ?.toUserModel()
            if (user != null) {
                usersDao.upsertAll(user)
            }
            return@withContext user
        }

    private suspend fun getUserFromRemoteSourceByEmail(email: String): UserModel? =
        withContext(Dispatchers.IO) {
            val query = firestoreHandle.whereEqualTo("email", email).limit(1)
            val result = query.get().await()
            val user = result.toObjects(UserDTO::class.java).firstOrNull()?.toUserModel()
            if (user != null) {
                usersDao.upsertAll(user)
            }
            return@withContext user
        }

    suspend fun getUsersFromRemoteSource(uids: List<String>): List<UserModel> =
        withContext(Dispatchers.IO) {
            val usersQuery =
                if (uids.isNotEmpty()) firestoreHandle.whereIn("id", uids) else firestoreHandle

            val users = usersQuery.get().await()
                .toObjects(UserDTO::class.java)
                .map { it.toUserModel() }

            if (users.isNotEmpty()) {
                usersDao.upsertAll(*users.toTypedArray())
            }
            return@withContext users
        }

    fun deleteAll() {
        usersDao.deleteAll()
    }
}