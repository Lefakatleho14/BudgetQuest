package com.budgetquest.app.ui.expense

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.budgetquest.app.R
import com.budgetquest.app.data.db.entity.Expense

class ExpenseAdapter(
    private var expenses: List<Expense>,
    private var categoryMap: Map<Int, String>
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvDateTime: TextView = itemView.findViewById(R.id.tvDateTime)
        val ivThumb: ImageView = itemView.findViewById(R.id.ivThumb)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]

        holder.tvAmount.text = "R %.2f".format(expense.amount)
        holder.tvDescription.text = expense.description
        holder.tvDateTime.text = "${expense.date} | ${expense.startTime} - ${expense.endTime}"

        // Category name lookup
        val categoryName = if (expense.categoryId != null) {
            categoryMap[expense.categoryId] ?: "Unknown"
        } else {
            "Uncategorised"
        }
        holder.tvCategory.text = categoryName

        // Image thumbnail
        if (!expense.imageUri.isNullOrEmpty()) {
            holder.ivThumb.visibility = View.VISIBLE
            try {
                holder.ivThumb.setImageURI(Uri.parse(expense.imageUri))
            } catch (e: Exception) {
                holder.ivThumb.visibility = View.GONE
            }
        } else {
            holder.ivThumb.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = expenses.size

    fun updateData(newExpenses: List<Expense>, newCategoryMap: Map<Int, String>) {
        expenses = newExpenses
        categoryMap = newCategoryMap
        notifyDataSetChanged()
    }
}