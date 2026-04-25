package com.budgetquest.app.data.repository

import com.budgetquest.app.data.db.dao.UserDao
import com.budgetquest.app.data.db.entity.User

class UserRepository(private val userDao: UserDao) {

    suspend fun register(username: String, password: String): Result<Long> {
        return try {
            val user = User(username = username.trim(), password = password)
            val id = userDao.insertUser(user)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(Exception("Username already exists. Please choose another."))
        }
    }

    suspend fun login(username: String, password: String): User? {
        return userDao.login(username.trim(), password)
    }

    suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username.trim())
    }

    suspend fun getUserById(id: Int): User? {
        return userDao.getUserById(id)
    }
}