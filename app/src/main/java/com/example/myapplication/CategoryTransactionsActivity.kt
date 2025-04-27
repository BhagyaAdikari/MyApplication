package com.example.myapplication

import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.widget.ImageView
import android.app.DatePickerDialog
import android.app.Dialog
import android.widget.DatePicker
import android.view.View
import androidx.fragment.app.DialogFragment
import java.text.SimpleDateFormat
import java.util.*

class CategoryTransactionsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private lateinit var tvCategoryTitle: TextView
    private lateinit var tvCategoryTotal: TextView
    private lateinit var tvTransactionCount: TextView
    private lateinit var tvSelectedMonth: TextView
    private val transactions = mutableListOf<Transaction>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private var selectedDate = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_transactions)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Initialize views
        recyclerView = findViewById(R.id.rvTransactions)
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle)
        tvCategoryTotal = findViewById(R.id.tvCategoryTotal)
        tvTransactionCount = findViewById(R.id.tvTransactionCount)
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth)

        // Get category from intent
        val categoryName = intent.getStringExtra("category_name") ?: return
        tvCategoryTitle.text = categoryName

        // Set current month
        updateSelectedMonthText()

        // Get currency from SharedPreferences
        val sharedPreferences = getSharedPreferences("finance_prefs", MODE_PRIVATE)
        val currency = sharedPreferences.getString("currency", "$") ?: "$"

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TransactionAdapter(
            transactions = transactions,
            onItemClick = { transaction ->
                // Show transaction details or edit dialog if needed
            },
            onItemLongClick = { transaction ->
                // Show delete confirmation if needed
            }
        ).apply {
            this.currency = currency
        }
        recyclerView.adapter = adapter

        // Add item decoration for spacing
        recyclerView.addItemDecoration(
            androidx.recyclerview.widget.DividerItemDecoration(
                this,
                androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
            )
        )

        // Setup click listeners
        findViewById<ImageView>(R.id.btnShowAnalysis).setOnClickListener {
            val intent = Intent(this, CategoryAnalysisActivity::class.java).apply {
                putExtra("category_name", categoryName)
            }
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.btnSelectMonth).setOnClickListener {
            showMonthYearPicker()
        }

        // Load transactions
        loadCategoryTransactions(categoryName)
    }

    private fun updateSelectedMonthText() {
        tvSelectedMonth.text = monthFormat.format(selectedDate.time)
    }

    private fun showMonthYearPicker() {
        val monthYearPickerDialog = MonthYearPickerDialog()
        monthYearPickerDialog.setListener { view, year, month, dayOfMonth ->
            selectedDate.set(Calendar.YEAR, year)
            selectedDate.set(Calendar.MONTH, month)
            updateSelectedMonthText()
            loadCategoryTransactions(tvCategoryTitle.text.toString())
        }
        monthYearPickerDialog.show(supportFragmentManager, "MonthYearPickerDialog")
    }

    private fun loadCategoryTransactions(categoryName: String) {
        val sharedPreferences = getSharedPreferences("finance_prefs", MODE_PRIVATE)
        val currency = sharedPreferences.getString("currency", "$") ?: "$"
        val transactionsJson = sharedPreferences.getString("transactions", "[]")
        val type = object : TypeToken<List<Transaction>>() {}.type
        val allTransactions = Gson().fromJson<List<Transaction>>(transactionsJson, type)

        // Filter transactions by category and selected month
        val startOfMonth = selectedDate.clone() as Calendar
        startOfMonth.set(Calendar.DAY_OF_MONTH, 1)
        val endOfMonth = selectedDate.clone() as Calendar
        endOfMonth.set(Calendar.DAY_OF_MONTH, endOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH))

        val categoryTransactions = allTransactions
            .filter { transaction ->
                val transactionDate = dateFormat.parse(transaction.date)
                transaction.category == categoryName &&
                transactionDate in startOfMonth.time..endOfMonth.time
            }
            .sortedByDescending { it.date }
        
        // Calculate totals
        val totalIncome = categoryTransactions
            .filter { it.type == "Income" }
            .sumOf { it.amount }
            
        val totalExpense = categoryTransactions
            .filter { it.type == "Expense" }
            .sumOf { it.amount }

        // Update UI
        tvCategoryTotal.text = "Income: $currency${String.format("%.2f", totalIncome)}\n" +
                             "Expense: $currency${String.format("%.2f", totalExpense)}"
        tvTransactionCount.text = "${categoryTransactions.size} transactions"

        // Update RecyclerView
        transactions.clear()
        transactions.addAll(categoryTransactions)
        adapter.notifyDataSetChanged()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

class MonthYearPickerDialog : DialogFragment() {
    private var listener: DatePickerDialog.OnDateSetListener? = null

    fun setListener(listener: DatePickerDialog.OnDateSetListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            listener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Hide day picker
        try {
            val datePickerField = datePickerDialog.javaClass.getDeclaredField("mDatePicker")
            datePickerField.isAccessible = true
            val datePicker = datePickerField.get(datePickerDialog) as DatePicker
            val daySpinner = datePicker.findViewById<View>(
                resources.getIdentifier("day", "id", "android")
            )
            daySpinner.visibility = View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return datePickerDialog
    }
} 