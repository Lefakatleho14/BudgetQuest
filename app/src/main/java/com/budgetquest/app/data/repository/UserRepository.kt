package com.budgetquest.app.data.repository

import com.budgetquest.app.data.db.dao.UserDao
import com.budgetquest.app.data.db.entity.User

class UserRepository(private val userDao: UserDao) {

    suspend fun register(username: String, password: String): Result<Long> {
        return try {
            // Hardcoded admin account — anyone registering with this exact
            // username becomes an admin user
            val role = if (username.trim().equals("admin", ignoreCase = true)) {
                "admin"
            } else {
                "user"
            }

            val user = User(
                username = username.trim(),
                password = password,
                role = role
            )
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

    suspend fun getAllNormalUsers(): List<User> {
        return userDao.getAllNormalUsers()
    }
}