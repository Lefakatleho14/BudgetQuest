package com.budgetquest.app.ui.reports

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.budgetquest.app.R

// Simple data class to hold a resolved category total row
data class CategoryTotalItem(
    val categoryName: String,
    val total: Double,
    val percentage: Int   // 0–100
)

class CategoryTotalAdapter(
    private var items: List<CategoryTotalItem>
) : RecyclerView.Adapter<CategoryTotalAdapter.TotalViewHolder>() {

    inner class TotalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        val tvPercentage: TextView = itemView.findViewById(R.id.tvPercentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TotalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_total, parent, false)
        return TotalViewHolder(view)
    }

    override fun onBindViewHolder(holder: TotalViewHolder, position: Int) {
        val item = items[position]
        holder.tvCategoryName.text = item.categoryName
        holder.tvTotal.text = "R %.2f".format(item.total)
        holder.progressBar.progress = item.percentage
        holder.tvPercentage.text = "${item.percentage}% of total"
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<CategoryTotalItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}