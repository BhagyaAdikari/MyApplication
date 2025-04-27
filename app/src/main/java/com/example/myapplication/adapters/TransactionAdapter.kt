package com.example.myapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.models.Transaction
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val transactions: List<Transaction>,
    private val onItemClick: (Transaction) -> Unit,
    private val onItemLongClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryIcon: ImageView = view.findViewById(R.id.categoryIcon)
        val titleText: TextView = view.findViewById(R.id.titleText)
        val dateText: TextView = view.findViewById(R.id.dateText)
        val amountText: TextView = view.findViewById(R.id.amountText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        holder.titleText.text = transaction.title
        holder.dateText.text = dateFormat.format(transaction.date)
        holder.amountText.text = String.format("$%.2f", transaction.amount)
        holder.amountText.setTextColor(
            holder.itemView.context.getColor(
                if (transaction.type == "Income") R.color.income_green else R.color.expense_red
            )
        )

        // Set category icon based on category
        val iconResId = when (transaction.category.lowercase()) {
            "food" -> R.drawable.cutlery
            "transport" -> R.drawable.transportation
            "shopping" -> R.drawable.shopping
            "bills" -> R.drawable.payment
            "entertainment" -> R.drawable.cinema
            "health" -> R.drawable.health
            "education" -> R.drawable.education
            "salary" -> R.drawable.add_file
            "other" -> R.drawable.other
            else -> R.drawable.ic_category
        }
        holder.categoryIcon.setImageResource(iconResId)

        // Set click listeners
        holder.itemView.setOnClickListener {
            onItemClick(transaction)
        }
        
        holder.itemView.setOnLongClickListener {
            onItemLongClick(transaction)
            true
        }
    }

    override fun getItemCount() = transactions.size
} 