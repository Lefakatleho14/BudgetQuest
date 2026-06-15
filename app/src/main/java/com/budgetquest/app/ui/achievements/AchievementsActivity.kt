package com.budgetquest.app.ui.achievements

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.budgetquest.app.R
import com.budgetquest.app.data.db.BudgetQuestDatabase
import com.budgetquest.app.data.repository.AchievementRepository
import com.budgetquest.app.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AchievementsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        // Dependencies
        val db = BudgetQuestDatabase.getDatabase(this)
        val achievementRepository = AchievementRepository(db.achievementDao(), db.userDao())
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()

        // Views
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val tvLevelCircle = findViewById<TextView>(R.id.tvLevelCircle)
        val tvLevelLabel = findViewById<TextView>(R.id.tvLevelLabel)
        val tvXpLabel = findViewById<TextView>(R.id.tvXpLabel)
        val progressXp = findViewById<ProgressBar>(R.id.progressXp)
        val tvStreak = findViewById<TextView>(R.id.tvStreak)
        val tvBadgeCount = findViewById<TextView>(R.id.tvBadgeCount)
        val rvBadges = findViewById<RecyclerView>(R.id.rvBadges)

        // Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Adapter
        val adapter = BadgeAdapter(emptyList())
        rvBadges.layoutManager = LinearLayoutManager(this)
        rvBadges.adapter = adapter

        // ── LOAD DATA ─────────────────────────────────────────────────────────
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                db.userDao().getUserById(userId)
            }

            val allAchievements = withContext(Dispatchers.IO) {
                achievementRepository.getAllAchievements()
            }

            val unlockedIds = withContext(Dispatchers.IO) {
                achievementRepository.getUnlockedIds(userId)
            }

            // Get unlock timestamps for display
            val userAchievements = withContext(Dispatchers.IO) {
                db.achievementDao().getUserAchievementsFlow(userId)
            }

            if (user != null) {
                // Level + XP display
                tvLevelCircle.text = user.level.toString()
                tvLevelLabel.text = "Level ${user.level}"

                // XP within current level (0-100 range per level)
                val xpInCurrentLevel = user.xp % 100
                tvXpLabel.text = "$xpInCurrentLevel / 100 XP"
                progressXp.progress = xpInCurrentLevel

                // Streak display
                tvStreak.text = "🔥 ${user.currentStreak} day streak " +
                        "(longest: ${user.longestStreak})"
            }

            // Badge count
            tvBadgeCount.text = "🏆 ${unlockedIds.size} / ${allAchievements.size} badges unlocked"

            // Build badge list — unlocked first, then locked
            val unlockedTimestamps = withContext(Dispatchers.IO) {
                db.achievementDao().getUserAchievementsFlow(userId)
            }

            // Get the actual unlocked records once for timestamps
            val unlockedRecords = withContext(Dispatchers.IO) {
                achievementRepository.getUnlockedIds(userId)
            }

            // Build a simple map of achievementId -> unlocked (we re-query for timestamps)
            val unlockedMap = withContext(Dispatchers.IO) {
                val records = mutableMapOf<String, String>()
                allAchievements.forEach { achievement ->
                    if (achievement.id in unlockedIds) {
                        records[achievement.id] = "Unlocked"
                    }
                }
                records
            }

            val badgeItems = allAchievements
                .sortedBy { if (it.id in unlockedIds) 0 else 1 } // unlocked first
                .map { achievement ->
                    BadgeItem(
                        achievement = achievement,
                        isUnlocked = achievement.id in unlockedIds,
                        unlockedAt = unlockedMap[achievement.id]
                    )
                }

            adapter.updateData(badgeItems)
        }
    }
}