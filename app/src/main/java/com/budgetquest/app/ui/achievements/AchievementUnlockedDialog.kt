package com.budgetquest.app.ui.achievements

import android.app.Dialog
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.budgetquest.app.R
import com.budgetquest.app.data.repository.UnlockedAchievementResult

object AchievementUnlockedDialog {

    // Shows a celebratory popup for a newly unlocked achievement.
    // If newLevel is non-null, also shows the level-up message.
    fun show(context: Context, result: UnlockedAchievementResult) {
        val view = View.inflate(context, R.layout.dialog_achievement_unlocked, null)

        val tvBadgeTitle = view.findViewById<TextView>(R.id.tvBadgeTitle)
        val tvBadgeDescription = view.findViewById<TextView>(R.id.tvBadgeDescription)
        val tvXpReward = view.findViewById<TextView>(R.id.tvXpReward)
        val tvLevelUp = view.findViewById<TextView>(R.id.tvLevelUp)
        val btnClose = view.findViewById<Button>(R.id.btnClose)

        tvBadgeTitle.text = result.achievement.title
        tvBadgeDescription.text = result.achievement.description
        tvXpReward.text = "+${result.achievement.xpReward} XP"

        if (result.newLevel != null) {
            tvLevelUp.text = "🎉 Level Up! You reached Level ${result.newLevel}"
            tvLevelUp.visibility = View.VISIBLE
        } else {
            tvLevelUp.visibility = View.GONE
        }

        val dialog: Dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(true)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // Shows multiple unlocked achievements one after another
    fun showAll(context: Context, results: List<UnlockedAchievementResult>) {
        if (results.isEmpty()) return
        // Show the first one — for simplicity in a student project we just
        // show one popup per save action even if multiple unlocked at once
        show(context, results.first())
    }
}