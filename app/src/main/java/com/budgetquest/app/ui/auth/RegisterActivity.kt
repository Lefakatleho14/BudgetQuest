package com.budgetquest.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.budgetquest.app.R
import com.budgetquest.app.data.db.BudgetQuestDatabase
import com.budgetquest.app.data.repository.UserRepository
import com.budgetquest.app.ui.dashboard.DashboardActivity
import com.budgetquest.app.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val db = BudgetQuestDatabase.getDatabase(this)
        val userRepository = UserRepository(db.userDao())
        val sessionManager = SessionManager(this)

        val ibBack = findViewById<ImageButton>(R.id.ibBack)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvError = findViewById<TextView>(R.id.tvError)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)

        ibBack.setOnClickListener { finish() }

        tvGoToLogin.setOnClickListener { finish() }

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (username.isEmpty()) {
                tvError.text = "Username cannot be empty."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (username.length < 3) {
                tvError.text = "Username must be at least 3 characters."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                tvError.text = "Password cannot be empty."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (password.length < 4) {
                tvError.text = "Password must be at least 4 characters."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                tvError.text = "Passwords do not match."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            tvError.visibility = View.GONE

            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    userRepository.register(username, password)
                }
                result.fold(
                    onSuccess = { userId ->
                        sessionManager.saveSession(userId.toInt(), username)
                        val intent = Intent(this@RegisterActivity, DashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    },
                    onFailure = { exception ->
                        tvError.text = exception.message ?: "Registration failed. Please try again."
                        tvError.visibility = View.VISIBLE
                        btnRegister.isEnabled = true
                    }
                )
            }
        }
    }
}