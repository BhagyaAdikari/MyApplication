package com.example.myapplication

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class CategoryReportActivity : AppCompatActivity() {
    private lateinit var tvCategoryName: TextView
    private lateinit var tvTotalTransactions: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvNetAmount: TextView
    private lateinit var tvAverageTransaction: TextView
    private lateinit var tvHighestTransaction: TextView
    private lateinit var tvLowestTransaction: TextView
    private lateinit var lineChart: LineChart
    private lateinit var pieChart: PieChart
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var currency: String = "$"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_report)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Initialize views
        initializeViews()

        // Get category name from intent
        val categoryName = intent.getStringExtra("category_name") ?: return
        tvCategoryName.text = categoryName

        // Get currency from SharedPreferences
        val sharedPreferences = getSharedPreferences("finance_prefs", MODE_PRIVATE)
        currency = sharedPreferences.getString("currency", "$") ?: "$"

        // Load and display report
        loadCategoryReport(categoryName)
    }

    private fun initializeViews() {
        tvCategoryName = findViewById(R.id.tvCategoryName)
        tvTotalTransactions = findViewById(R.id.tvTotalTransactions)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvTotalExpense = findViewById(R.id.tvTotalExpense)
        tvNetAmount = findViewById(R.id.tvNetAmount)
        tvAverageTransaction = findViewById(R.id.tvAverageTransaction)
        tvHighestTransaction = findViewById(R.id.tvHighestTransaction)
        tvLowestTransaction = findViewById(R.id.tvLowestTransaction)
        lineChart = findViewById(R.id.lineChart)
        pieChart = findViewById(R.id.pieChart)
    }

    private fun loadCategoryReport(categoryName: String) {
        val sharedPreferences = getSharedPreferences("finance_prefs", MODE_PRIVATE)
        val transactionsJson = sharedPreferences.getString("transactions", "[]")
        val type = object : TypeToken<List<Transaction>>() {}.type
        val allTransactions = Gson().fromJson<List<Transaction>>(transactionsJson, type)

        // Filter transactions for this category
        val categoryTransactions = allTransactions
            .filter { it.category == categoryName }
            .sortedBy { it.date }

        if (categoryTransactions.isEmpty()) {
            // Show empty state
            showEmptyState()
            return
        }

        // Calculate statistics
        val totalIncome = categoryTransactions.filter { it.type == "Income" }.sumOf { it.amount }
        val totalExpense = categoryTransactions.filter { it.type == "Expense" }.sumOf { it.amount }
        val netAmount = totalIncome - totalExpense
        val averageAmount = categoryTransactions.map { it.amount }.average()
        val highestAmount = categoryTransactions.maxOf { it.amount }
        val lowestAmount = categoryTransactions.minOf { it.amount }

        // Update UI with statistics
        updateStatistics(
            totalTransactions = categoryTransactions.size,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netAmount = netAmount,
            averageAmount = averageAmount,
            highestAmount = highestAmount,
            lowestAmount = lowestAmount
        )

        // Setup charts
        setupLineChart(categoryTransactions)
        setupPieChart(totalIncome, totalExpense)
    }

    private fun updateStatistics(
        totalTransactions: Int,
        totalIncome: Double,
        totalExpense: Double,
        netAmount: Double,
        averageAmount: Double,
        highestAmount: Double,
        lowestAmount: Double
    ) {
        tvTotalTransactions.text = "Total Transactions: $totalTransactions"
        tvTotalIncome.text = "Total Income: $currency${String.format("%.2f", totalIncome)}"
        tvTotalExpense.text = "Total Expense: $currency${String.format("%.2f", totalExpense)}"
        tvNetAmount.text = "Net Amount: $currency${String.format("%.2f", netAmount)}"
        tvAverageTransaction.text = "Average Amount: $currency${String.format("%.2f", averageAmount)}"
        tvHighestTransaction.text = "Highest Transaction: $currency${String.format("%.2f", highestAmount)}"
        tvLowestTransaction.text = "Lowest Transaction: $currency${String.format("%.2f", lowestAmount)}"
    }

    private fun setupLineChart(transactions: List<Transaction>) {
        val entries = transactions.mapIndexed { index, transaction ->
            Entry(index.toFloat(), transaction.amount.toFloat())
        }

        val dataSet = LineDataSet(entries, "Transaction Amount").apply {
            color = resources.getColor(R.color.primary_purple, theme)
            setDrawCircles(true)
            setDrawValues(false)
            lineWidth = 2f
        }

        lineChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            axisRight.isEnabled = false
            legend.isEnabled = true
            animateX(1000)
            invalidate()
        }
    }

    private fun setupPieChart(totalIncome: Double, totalExpense: Double) {
        val entries = listOf(
            PieEntry(totalIncome.toFloat(), "Income"),
            PieEntry(totalExpense.toFloat(), "Expense")
        )

        val dataSet = PieDataSet(entries, "Income vs Expense").apply {
            colors = listOf(
                resources.getColor(R.color.income_green, theme),
                resources.getColor(R.color.expense_red, theme)
            )
        }

        pieChart.apply {
            data = PieData(dataSet).apply {
                setValueFormatter(PercentFormatter(pieChart))
                setValueTextSize(11f)
            }
            description.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelTextSize(12f)
            animateY(1000)
            invalidate()
        }
    }

    private fun showEmptyState() {
        // Implement empty state UI
        tvTotalTransactions.text = "No transactions found"
        lineChart.setNoDataText("No transaction data available")
        pieChart.setNoDataText("No transaction data available")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 