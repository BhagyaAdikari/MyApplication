package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class CategoryInfo(
    val name: String,
    val transactionCount: Int,
    val totalAmount: Double,
    val iconResId: Int
)

class CategoryAdapter(
    private val categories: List<CategoryInfo>,
    private val onCategoryClick: (CategoryInfo) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvTransactionCount: TextView = itemView.findViewById(R.id.tvTransactionCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        
        // Set category name with proper capitalization
        holder.tvCategoryName.text = category.name.replaceFirstChar { it.uppercase() }
        
        // Set transaction count with proper formatting
        val transactionText = when (category.transactionCount) {
            0 -> "No transactions"
            1 -> "1 transaction"
            else -> "${category.transactionCount} transactions"
        }
        
        // Add total amount if there are transactions
        val displayText = if (category.transactionCount > 0) {
            "$transactionText • Total: $${String.format("%.2f", category.totalAmount)}"
        } else {
            transactionText
        }
        
        holder.tvTransactionCount.text = displayText
        holder.ivCategoryIcon.setImageResource(category.iconResId)

        holder.itemView.setOnClickListener {
            onCategoryClick(category)
        }
    }

    override fun getItemCount() = categories.size
} 