package com.example.myapplication

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(
    private val transactions: MutableList<Transaction>,
    private val onItemClick: (Transaction) -> Unit,
    private val onItemLongClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    var currency: String = "$" // Add currency as a mutable property

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.transaction_item, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.tvTitle.text = transaction.title
        holder.tvAmount.text = "$currency%.2f".format(transaction.amount) // Add currency symbol
        holder.tvAmount.setTextColor(
            if (transaction.type == "Income") Color.GREEN else Color.RED
        )
        holder.tvCategory.text = transaction.category
        holder.tvDate.text = transaction.date

        holder.itemView.setOnClickListener { onItemClick(transaction) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(transaction)
            true
        }
    }

    override fun getItemCount() = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions.clear()
        transactions.addAll(newTransactions)
        notifyDataSetChanged()
    }
}