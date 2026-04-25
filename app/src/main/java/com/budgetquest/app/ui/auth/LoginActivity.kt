package com.budgetquest.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
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

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val db = BudgetQuestDatabase.getDatabase(this)
        val userRepository = UserRepository(db.userDao())
        val sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            goToDashboard()
            return
        }

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvError = findViewById<TextView>(R.id.tvError)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()

            if (username.isEmpty()) {
                tvError.text = "Username cannot be empty."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                tvError.text = "Password cannot be empty."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            tvError.visibility = View.GONE

            lifecycleScope.launch {
                val user = withContext(Dispatchers.IO) {
                    userRepository.login(username, password)
                }
                if (user != null) {
                    sessionManager.saveSession(user.id, user.username)
                    goToDashboard()
                } else {
                    tvError.text = "Invalid username or password. Please try again."
                    tvError.visibility = View.VISIBLE
                    btnLogin.isEnabled = true
                }
            }
        }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun goToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}