package com.example.myapplication

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapters.TransactionAdapter
import com.example.myapplication.database.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class FullReportActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var totalExpenseText: TextView
    private lateinit var dateRangeText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_report)

        // Initialize views
        recyclerView = findViewById(R.id.reportRecyclerView)
        totalExpenseText = findViewById(R.id.totalExpenseText)
        dateRangeText = findViewById(R.id.dateRangeText)

        // Set up database helper
        dbHelper = DatabaseHelper(this)

        // Set up action bar
        supportActionBar?.apply {
            title = "Full Expense Report"
            setDisplayHomeAsUpEnabled(true)
        }

        loadReport()
    }

    private fun loadReport() {
        // Get all transactions
        val transactions = dbHelper.getAllTransactions()
        
        // Calculate total expense
        val totalExpense = transactions.sumOf { it.amount }
        
        // Get date range
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val firstDate = if (transactions.isNotEmpty()) dateFormat.format(transactions.minOf { it.date }) else "N/A"
        val lastDate = if (transactions.isNotEmpty()) dateFormat.format(transactions.maxOf { it.date }) else "N/A"
        
        // Update UI
        totalExpenseText.text = String.format("Total Expense: $%.2f", totalExpense)
        dateRangeText.text = "Period: $firstDate to $lastDate"
        
        // Set up RecyclerView
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@FullReportActivity)
            adapter = TransactionAdapter(
                transactions = transactions,
                onItemClick = { transaction ->
                    // Handle item click
                },
                onItemLongClick = { transaction ->
                    // Handle item long click
                }
            )
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 