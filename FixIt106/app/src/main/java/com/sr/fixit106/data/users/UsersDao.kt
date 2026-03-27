package com.sr.fixit106.data.users

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert

@Dao
interface UsersDao {
    @Query("SELECT * FROM users WHERE id LIKE :id LIMIT 1")
    suspend fun getUserByUid(id: String): UserModel?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeUserByUid(id: String): LiveData<UserModel?>

    @Query("SELECT * FROM users WHERE email LIKE :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserModel?

    @Query("SELECT id FROM users WHERE id IN (:ids)")
    suspend fun getExistingUserIds(ids: List<String>): List<String>

    @Upsert
    fun upsertAll(vararg users: UserModel)

    @Query("DELETE FROM users WHERE id = :id")
    fun deleteByUid(id: String)

    @Update
    fun updateUserData(user: UserModel)

    @Query("DELETE FROM users")
    fun deleteAll()
}