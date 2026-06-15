package com.budgetquest.app.ui.achievements

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.budgetquest.app.R
import com.budgetquest.app.data.db.entity.Achievement

// Represents a badge with its unlock status for display
data class BadgeItem(
    val achievement: Achievement,
    val isUnlocked: Boolean,
    val unlockedAt: String? // null if locked
)

class BadgeAdapter(
    private var badges: List<BadgeItem>
) : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    inner class BadgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBadgeIcon: TextView = itemView.findViewById(R.id.tvBadgeIcon)
        val tvBadgeTitle: TextView = itemView.findViewById(R.id.tvBadgeTitle)
        val tvBadgeDescription: TextView = itemView.findViewById(R.id.tvBadgeDescription)
        val tvBadgeStatus: TextView = itemView.findViewById(R.id.tvBadgeStatus)
        val tvXpReward: TextView = itemView.findViewById(R.id.tvXpReward)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_badge, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val item = badges[position]

        holder.tvBadgeTitle.text = item.achievement.title
        holder.tvBadgeDescription.text = item.achievement.description
        holder.tvXpReward.text = "+${item.achievement.xpReward} XP"

        if (item.isUnlocked) {
            holder.tvBadgeIcon.text = "🏆"
            holder.tvBadgeIcon.background = ContextCompat.getDrawable(
                holder.itemView.context, R.drawable.bg_icon_circle_teal
            )
            holder.tvBadgeStatus.text = "Unlocked on ${item.unlockedAt}"
            holder.tvBadgeStatus.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.success_green)
            )
            holder.tvBadgeTitle.alpha = 1.0f
            holder.tvBadgeDescription.alpha = 1.0f
        } else {
            holder.tvBadgeIcon.text = "🔒"
            holder.tvBadgeIcon.background = ContextCompat.getDrawable(
                holder.itemView.context, R.drawable.bg_icon_circle_purple
            )
            holder.tvBadgeStatus.text = "Locked"
            holder.tvBadgeStatus.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.text_secondary)
            )
            holder.tvBadgeIcon.alpha = 0.5f
            holder.tvBadgeTitle.alpha = 0.5f
            holder.tvBadgeDescription.alpha = 0.5f
        }
    }

    override fun getItemCount(): Int = badges.size

    fun updateData(newBadges: List<BadgeItem>) {
        badges = newBadges
        notifyDataSetChanged()
    }
}