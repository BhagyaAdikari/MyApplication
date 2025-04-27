package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class CategoryAnalysisActivity : AppCompatActivity() {
    private lateinit var lineChart: LineChart
    private lateinit var pieChart: PieChart
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_analysis)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        val categoryName = intent.getStringExtra("category_name") ?: return
        supportActionBar?.title = "$categoryName Analysis"

        // Initialize charts
        lineChart = findViewById(R.id.lineChart)
        pieChart = findViewById(R.id.pieChart)

        // Load and display data
        loadTransactionData(categoryName)
    }

    private fun loadTransactionData(categoryName: String) {
        val sharedPreferences = getSharedPreferences("finance_prefs", MODE_PRIVATE)
        val transactionsJson = sharedPreferences.getString("transactions", "[]")
        val type = object : TypeToken<List<Transaction>>() {}.type
        val allTransactions = Gson().fromJson<List<Transaction>>(transactionsJson, type)

        // Filter transactions for this category
        val categoryTransactions = allTransactions.filter { it.category == categoryName }

        setupLineChart(categoryTransactions)
        setupPieChart(categoryTransactions)
    }

    private fun setupLineChart(transactions: List<Transaction>) {
        // Group transactions by month
        val monthlyData = transactions
            .groupBy { it.date.substring(0, 7) } // Group by YYYY-MM
            .mapKeys { monthFormat.format(dateFormat.parse(it.key + "-01")!!) }
            .mapValues { it.value.sumOf { transaction -> 
                if (transaction.type == "Expense") -transaction.amount else transaction.amount 
            }}
            .toSortedMap()

        val entries = monthlyData.values.mapIndexed { index, amount ->
            Entry(index.toFloat(), amount.toFloat())
        }

        val dataSet = LineDataSet(entries, "Monthly Net Amount").apply {
            color = Color.BLUE
            setCircleColor(Color.BLUE)
            lineWidth = 2f
            circleRadius = 4f
            valueTextSize = 10f
        }

        lineChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(monthlyData.keys.toList())
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                labelRotationAngle = -45f
            }
            axisLeft.apply {
                axisMinimum = monthlyData.values.minOrNull()?.toFloat()?.times(1.1f) ?: 0f
                axisMaximum = monthlyData.values.maxOrNull()?.toFloat()?.times(1.1f) ?: 0f
            }
            axisRight.isEnabled = false
            legend.isEnabled = true
            animateY(1000)
        }
    }

    private fun setupPieChart(transactions: List<Transaction>) {
        // Separate income and expenses
        val income = transactions.filter { it.type == "Income" }.sumOf { it.amount }
        val expenses = transactions.filter { it.type == "Expense" }.sumOf { it.amount }

        val entries = listOf(
            PieEntry(income.toFloat(), "Income"),
            PieEntry(expenses.toFloat(), "Expenses")
        )

        val dataSet = PieDataSet(entries, "Income vs Expenses").apply {
            colors = listOf(Color.GREEN, Color.RED)
            valueTextSize = 14f
            valueTextColor = Color.WHITE
        }

        pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 35f
            transparentCircleRadius = 40f
            setUsePercentValues(true)
            setEntryLabelColor(Color.WHITE)
            legend.isEnabled = true
            animateY(1000)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 