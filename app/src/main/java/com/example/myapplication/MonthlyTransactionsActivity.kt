package com.example.myapplication

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapters.TransactionAdapter
import com.example.myapplication.database.DatabaseHelper
import com.example.myapplication.models.Transaction
import java.text.SimpleDateFormat
import java.util.*

class MonthlyTransactionsActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvMonthYear: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvBalance: TextView
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var currentMonth: Int = 0
    private var currentYear: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_transactions)

        // Initialize views
        recyclerView = findViewById(R.id.rvTransactions)
        tvMonthYear = findViewById(R.id.tvMonthYear)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvTotalExpense = findViewById(R.id.tvTotalExpense)
        tvBalance = findViewById(R.id.tvBalance)

        // Set up database helper
        dbHelper = DatabaseHelper(this)

        // Get selected month and year from intent
        currentMonth = intent.getIntExtra("month", Calendar.getInstance().get(Calendar.MONTH))
        currentYear = intent.getIntExtra("year", Calendar.getInstance().get(Calendar.YEAR))

        // Set up action bar
        supportActionBar?.apply {
            title = "Monthly Transactions"
            setDisplayHomeAsUpEnabled(true)
        }

        loadMonthlyTransactions(currentMonth, currentYear)
    }

    private fun loadMonthlyTransactions(month: Int, year: Int) {
        // Get all transactions
        val allTransactions = dbHelper.getAllTransactions()
        
        // Filter transactions for the selected month and year
        val monthlyTransactions = allTransactions.filter { transaction ->
            val cal = Calendar.getInstance()
            cal.time = transaction.date
            cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
        }

        // Calculate totals
        val totalIncome = monthlyTransactions.filter { it.type == "Income" }.sumOf { it.amount }
        val totalExpense = monthlyTransactions.filter { it.type == "Expense" }.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        // Update UI
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        tvMonthYear.text = monthYearFormat.format(cal.time)
        
        tvTotalIncome.text = String.format("Total Income: $%.2f", totalIncome)
        tvTotalExpense.text = String.format("Total Expense: $%.2f", totalExpense)
        tvBalance.text = String.format("Balance: $%.2f", balance)

        // Set up RecyclerView
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MonthlyTransactionsActivity)
            adapter = TransactionAdapter(
                transactions = monthlyTransactions,
                onItemClick = { transaction ->
                    Log.d("MonthlyTransactions", "Item clicked: ${transaction.title}")
                    showTransactionOptions(transaction)
                },
                onItemLongClick = { transaction ->
                    Log.d("MonthlyTransactions", "Item long clicked: ${transaction.title}")
                    showTransactionOptions(transaction)
                }
            )
        }
    }

    private fun showTransactionOptions(transaction: Transaction) {
        Log.d("MonthlyTransactions", "Showing options for transaction: ${transaction.title}")
        val options = arrayOf("Update", "Delete", "Cancel")
        AlertDialog.Builder(this)
            .setTitle("Transaction Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        Log.d("MonthlyTransactions", "Update selected")
                        showUpdateDialog(transaction)
                    }
                    1 -> {
                        Log.d("MonthlyTransactions", "Delete selected")
                        showDeleteConfirmation(transaction)
                    }
                }
            }
            .show()
    }

    private fun showUpdateDialog(transaction: Transaction) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null)
        var selectedDate = transaction.date

        with(dialogView) {
            findViewById<TextView>(R.id.tvSelectedDate).text = dateFormat.format(selectedDate)
            findViewById<Button>(R.id.btnSelectDate).setOnClickListener {
                showDatePickerDialog { y, m, d ->
                    selectedDate = Calendar.getInstance().apply { set(y, m, d) }.time
                    findViewById<TextView>(R.id.tvSelectedDate).text = dateFormat.format(selectedDate)
                }
            }

            findViewById<EditText>(R.id.etTitle).setText(transaction.title)
            findViewById<EditText>(R.id.etAmount).setText(transaction.amount.toString())

            if (transaction.type == "Income") {
                findViewById<RadioButton>(R.id.rbIncome).isChecked = true
            } else {
                findViewById<RadioButton>(R.id.rbExpense).isChecked = true
            }

            val spinner = findViewById<Spinner>(R.id.spCategory)
            spinner.adapter = ArrayAdapter(
                this@MonthlyTransactionsActivity,
                android.R.layout.simple_spinner_dropdown_item,
                resources.getStringArray(R.array.categories)
            )
            spinner.setSelection(resources.getStringArray(R.array.categories).indexOf(transaction.category))
        }

        AlertDialog.Builder(this)
            .setTitle("Update Transaction")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                with(dialogView) {
                    val title = findViewById<EditText>(R.id.etTitle).text.toString()
                    val amount = findViewById<EditText>(R.id.etAmount).text.toString().toDoubleOrNull() ?: 0.0
                    val category = findViewById<Spinner>(R.id.spCategory).selectedItem.toString()
                    val type = if (findViewById<RadioButton>(R.id.rbIncome).isChecked) "Income" else "Expense"

                    if (title.isNotEmpty() && amount > 0) {
                        val updatedTransaction = transaction.copy(
                            title = title,
                            amount = amount,
                            category = category,
                            date = selectedDate,
                            type = type
                        )

                        // Update in database
                        dbHelper.updateTransaction(updatedTransaction)
                        
                        // Send broadcast to update MainActivity
                        LocalBroadcastManager.getInstance(this@MonthlyTransactionsActivity)
                            .sendBroadcast(Intent("TRANSACTION_UPDATED"))
                        
                        // Reload transactions
                        loadMonthlyTransactions(currentMonth, currentYear)
                        
                        Toast.makeText(this@MonthlyTransactionsActivity, "Transaction updated", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MonthlyTransactionsActivity, "Please enter valid details", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                // Delete from database
                dbHelper.deleteTransaction(transaction.id)
                
                // Send broadcast to update MainActivity
                LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(Intent("TRANSACTION_DELETED"))
                
                // Reload transactions
                loadMonthlyTransactions(currentMonth, currentYear)
                
                Toast.makeText(this, "Transaction deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDatePickerDialog(onDateSelected: (Int, Int, Int) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, y, m, d -> onDateSelected(y, m, d) },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}