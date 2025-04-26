package com.example.myapplication

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // Views
    private lateinit var rvCategories: RecyclerView
    private lateinit var summaryContainer: LinearLayout
    private lateinit var tvBudgetAmount: TextView
    private lateinit var progressBudget: ProgressBar
    private lateinit var tvBudgetStatus: TextView
    private lateinit var currencySpinner: Spinner
    private lateinit var btnBackup: MaterialCardView
    private lateinit var btnAddTransaction: MaterialCardView
    private lateinit var btnSetBudget: MaterialButton
    private lateinit var tvViewFullReport: TextView

    // Data
    private val transactions = mutableListOf<Transaction>()
    private var monthlyBudget: Double = 0.0
    private var currency: String = "$"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private lateinit var categoryAdapter: CategoryAdapter

    // Preferences
    private lateinit var prefs: SharedPreferences
    private val TRANSACTIONS_KEY = "transactions"
    private val BUDGET_KEY = "monthly_budget"
    private val CURRENCY_KEY = "currency"

    // Notification
    private val BUDGET_CHANNEL_ID = "budget_alerts"
    private val REMINDER_CHANNEL_ID = "daily_reminders"
    private val BUDGET_NOTIFICATION_ID = 1
    private val REMINDER_NOTIFICATION_ID = 2

    // Add these constants at the top of the class with other constants
    private val DAILY_REMINDER_CHANNEL_ID = "daily_transaction_reminder"
    private val DAILY_REMINDER_NOTIFICATION_ID = 3
    private val LAST_TRANSACTION_CHECK_KEY = "last_transaction_check"
    private val REMINDER_ENABLED_KEY = "reminder_enabled"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences first
        prefs = getSharedPreferences("finance_prefs", MODE_PRIVATE)
        
        // Then initialize views and other components
        initViews()
        setupRecyclerView()
        loadData()
        createNotificationChannels()
        setClickListeners()
        checkBudgetAlerts()
        checkDailyTransactions()
        
        // Only schedule reminders if enabled
        if (prefs.getBoolean(REMINDER_ENABLED_KEY, false)) {
            scheduleDailyReminder()
            scheduleDailyTransactionCheck()
        }
    }

    private fun initViews() {
        rvCategories = findViewById(R.id.rvCategories)
        summaryContainer = findViewById(R.id.summaryContainer)
        tvBudgetAmount = findViewById(R.id.tvBudgetAmount)
        progressBudget = findViewById(R.id.progressBudget)
        tvBudgetStatus = findViewById(R.id.tvBudgetStatus)
        currencySpinner = findViewById(R.id.spCurrency)
        btnBackup = findViewById(R.id.btnBackup)
        btnAddTransaction = findViewById(R.id.btnAddTransaction)
        btnSetBudget = findViewById(R.id.btnSetBudget)
        tvViewFullReport = findViewById(R.id.tvViewFullReport)

        // Set up reminder button
        val btnReminder = findViewById<ImageView>(R.id.btnReminder)
        val isReminderEnabled = prefs.getBoolean(REMINDER_ENABLED_KEY, false)
        btnReminder.setColorFilter(
            if (isReminderEnabled) 
                ContextCompat.getColor(this, R.color.primary_purple)
            else 
                ContextCompat.getColor(this, R.color.secondary_text)
        )
        
        btnReminder.setOnClickListener {
            showReminderDialog()
        }
    }

    private fun loadData() {
        val json = prefs.getString(TRANSACTIONS_KEY, null)
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<Transaction>>() {}.type
            transactions.addAll(Gson().fromJson(json, type))
        } else {
            addSampleData()
        }

        monthlyBudget = prefs.getFloat(BUDGET_KEY, 0f).toDouble()
        currency = prefs.getString(CURRENCY_KEY, "$") ?: "$"

        ArrayAdapter.createFromResource(
            this,
            R.array.currencies,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            currencySpinner.adapter = adapter
            currencySpinner.setSelection(
                resources.getStringArray(R.array.currencies)
                    .indexOfFirst { it.contains(currency) }
            )
        }
    }

    private fun saveData() {
        prefs.edit().apply {
            putString(TRANSACTIONS_KEY, Gson().toJson(transactions))
            putFloat(BUDGET_KEY, monthlyBudget.toFloat())
            putString(CURRENCY_KEY, currency)
            apply()
        }
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            categories = emptyList(),
            onCategoryClick = { category ->
                val intent = Intent(this, CategoryTransactionsActivity::class.java).apply {
                    putExtra("category_name", category.name)
                }
                startActivity(intent)
            }
        )

        rvCategories.apply {
            adapter = categoryAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setClickListeners() {
        btnAddTransaction.setOnClickListener {
            showAddTransactionDialog()
        }

        btnSetBudget.setOnClickListener {
            showSetBudgetDialog()
        }

        btnBackup.setOnClickListener {
            showBackupRestoreDialog()
        }

        tvViewFullReport.setOnClickListener {
            val intent = Intent(this, FullReportActivity::class.java)
            startActivity(intent)
        }

        currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currency = parent?.getItemAtPosition(position).toString().substringBefore(" ")
                saveData()
                updateAllViews()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateAllViews() {
        updateCategorySummary()
        updateBudgetViews()
        updateCategories()
        checkBudgetAlerts()
    }

    private fun updateCategorySummary() {
        summaryContainer.removeAllViews()

        val categoryMap = mutableMapOf<String, Double>()
        transactions.filter { it.type == "Expense" }
            .forEach {
                categoryMap[it.category] = (categoryMap[it.category] ?: 0.0) + it.amount
            }

        categoryMap.forEach { (category, total) ->
            val summaryView = LayoutInflater.from(this)
                .inflate(R.layout.item_category_summary, summaryContainer, false)

            summaryView.findViewById<TextView>(R.id.tvCategory).text = category
            summaryView.findViewById<TextView>(R.id.tvAmount).text =
                "$currency${"%.2f".format(total)}"

            summaryContainer.addView(summaryView)
        }
    }

    private fun updateBudgetViews() {
        val totalExpenses = transactions
            .filter { it.type == "Expense" }
            .sumOf { it.amount }

        tvBudgetAmount.text = if (monthlyBudget > 0) {
            "Budget: $currency${"%.2f".format(monthlyBudget)} | Spent: $currency${"%.2f".format(totalExpenses)}"
        } else {
            "No budget set"
        }

        if (monthlyBudget > 0) {
            val progress = ((totalExpenses / monthlyBudget) * 100).toInt().coerceAtMost(100)
            progressBudget.progress = progress

            when {
                totalExpenses >= monthlyBudget -> {
                    tvBudgetStatus.text = "Budget exceeded!"
                    tvBudgetStatus.setTextColor(Color.RED)
                }
                progress >= 80 -> {
                    tvBudgetStatus.text = "Approaching budget limit!"
                    tvBudgetStatus.setTextColor(ContextCompat.getColor(this, R.color.purple_200))
                }
                else -> {
                    tvBudgetStatus.text = "Within budget"
                    tvBudgetStatus.setTextColor(Color.GREEN)
                }
            }
        } else {
            progressBudget.progress = 0
            tvBudgetStatus.text = "No budget set"
            tvBudgetStatus.setTextColor(Color.GRAY)
        }
    }

    private fun updateCategories() {
        // Get all predefined categories
        val allCategories = resources.getStringArray(R.array.categories).toList()
        
        // Group transactions by category
        val categoryMap = mutableMapOf<String, MutableList<Transaction>>()
        transactions.forEach { transaction ->
            categoryMap.getOrPut(transaction.category) { mutableListOf() }.add(transaction)
        }

        // Create category info list for all categories
        val categories = allCategories.map { category ->
            val categoryTransactions = categoryMap[category] ?: emptyList()
            CategoryInfo(
                name = category,
                transactionCount = categoryTransactions.size,
                totalAmount = categoryTransactions.sumOf { it.amount },
                iconResId = getCategoryIcon(category)
            )
        }.sortedByDescending { it.totalAmount }

        // Update RecyclerView
        categoryAdapter = CategoryAdapter(
            categories = categories,
            onCategoryClick = { category ->
                val intent = Intent(this, CategoryTransactionsActivity::class.java).apply {
                    putExtra("category_name", category.name)
                }
                startActivity(intent)
            }
        )
        rvCategories.adapter = categoryAdapter
    }

    private fun getCategoryIcon(category: String): Int {
        return when (category.lowercase()) {
            "food" -> R.drawable.cutlery
            "transport" -> R.drawable.transportation
            "shopping" -> R.drawable.shopping
            "entertainment" -> R.drawable.cinema
            "bills" -> R.drawable.payment
            "health" -> R.drawable.health
            "education" -> R.drawable.education
            "income" -> R.drawable.income
            else -> R.drawable.other
        }
    }

    private fun checkBudgetAlerts() {
        if (monthlyBudget == 0.0) return

        val totalExpenses = transactions
            .filter { it.type == "Expense" }
            .sumOf { it.amount }

        when {
            totalExpenses >= monthlyBudget -> {
                showBudgetAlert("Budget Exceeded", "You've exceeded your monthly budget!")
                showBudgetNotification(
                    "Budget Exceeded",
                    "You've spent $currency${"%.2f".format(totalExpenses)} this month"
                )
            }
            totalExpenses >= monthlyBudget * 0.8 -> {
                showBudgetAlert("Budget Warning", "You've used 80% of your monthly budget")
                showBudgetNotification(
                    "Budget Warning",
                    "You've used 80% of your $currency${"%.2f".format(monthlyBudget)} budget"
                )
            }
        }
    }

    private fun showBudgetAlert(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun addSampleData() {
        transactions.addAll(
            listOf(
                Transaction(
                    title = "Salary", amount = 2500.0, category = "Income",
                    date = dateFormat.format(Date()), type = "Income"
                ),
                Transaction(
                    title = "Groceries", amount = 150.75, category = "Food",
                    date = dateFormat.format(Date()), type = "Expense"
                ),
                Transaction(
                    title = "Dinner", amount = 45.60, category = "Food",
                    date = dateFormat.format(Date()), type = "Expense"
                ),
                Transaction(
                    title = "Bus Pass", amount = 30.0, category = "Transport",
                    date = dateFormat.format(Date()), type = "Expense"
                )
            )
        )
        categoryAdapter.notifyDataSetChanged()
    }

    private fun showAddTransactionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null)
        var selectedDate = Date()

        with(dialogView) {
            findViewById<TextView>(R.id.tvSelectedDate).text = dateFormat.format(selectedDate)
            findViewById<Button>(R.id.btnSelectDate).setOnClickListener {
                showDatePickerDialog { y, m, d ->
                    selectedDate = Calendar.getInstance().apply { set(y, m, d) }.time
                    findViewById<TextView>(R.id.tvSelectedDate).text = dateFormat.format(selectedDate)
                }
            }

            val spinner = findViewById<Spinner>(R.id.spCategory)
            spinner.adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                resources.getStringArray(R.array.categories)
            )
        }

        AlertDialog.Builder(this)
            .setTitle("Add Transaction")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                with(dialogView) {
                    val title = findViewById<EditText>(R.id.etTitle).text.toString()
                    val amount = findViewById<EditText>(R.id.etAmount).text.toString().toDoubleOrNull() ?: 0.0
                    val category = findViewById<Spinner>(R.id.spCategory).selectedItem.toString()
                    val type = if (findViewById<RadioButton>(R.id.rbIncome).isChecked) "Income" else "Expense"

                    if (title.isNotEmpty() && amount > 0) {
                        transactions.add(0, Transaction(
                            title = title,
                            amount = amount,
                            category = category,
                            date = dateFormat.format(selectedDate),
                            type = type
                        ))
                        categoryAdapter.notifyItemInserted(0)
                        saveData()
                        updateAllViews()
                        rvCategories.scrollToPosition(0)
                    } else {
                        Toast.makeText(this@MainActivity, "Please enter valid details", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTransactionOptions(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Transaction Options")
            .setItems(arrayOf("Edit", "Delete", "Cancel")) { _, which ->
                when (which) {
                    0 -> showEditTransactionDialog(transaction)
                    1 -> deleteTransaction(transaction)
                }
            }
            .show()
    }

    private fun deleteTransaction(transaction: Transaction) {
        val position = transactions.indexOfFirst { it.id == transaction.id }
        if (position != -1) {
            transactions.removeAt(position)
            categoryAdapter.notifyItemRemoved(position)
            saveData()
            updateAllViews()
            Toast.makeText(this, "Transaction deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditTransactionDialog(transaction: Transaction) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null)
        var selectedDate = dateFormat.parse(transaction.date) ?: Date()

        with(dialogView) {
            findViewById<TextView>(R.id.tvSelectedDate).text = transaction.date
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
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                resources.getStringArray(R.array.categories)
            )
            spinner.setSelection(resources.getStringArray(R.array.categories).indexOf(transaction.category))
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Transaction")
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
                            date = dateFormat.format(selectedDate),
                            type = type
                        )

                        val position = transactions.indexOfFirst { it.id == transaction.id }
                        if (position != -1) {
                            transactions[position] = updatedTransaction
                            categoryAdapter.notifyItemChanged(position)
                            saveData()
                            updateAllViews()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Please enter valid details", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSetBudgetDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_set_budget, null)
        val etBudget = dialogView.findViewById<EditText>(R.id.etBudgetAmount)

        if (monthlyBudget > 0) {
            etBudget.setText(monthlyBudget.toString())
        }

        AlertDialog.Builder(this)
            .setTitle("Set Monthly Budget")
            .setView(dialogView)
            .setPositiveButton("Set") { _, _ ->
                val amount = etBudget.text.toString().toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    monthlyBudget = amount
                    saveData()
                    updateAllViews()
                } else {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBackupRestoreDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.backup_restore_dialog, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Backup & Restore")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        dialogView.findViewById<Button>(R.id.btnExport).setOnClickListener {
            exportData()
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnImport).setOnClickListener {
            importData()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun exportData() {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "finance_backup_${System.currentTimeMillis()}.json")
            FileOutputStream(file).use { stream ->
                stream.write(Gson().toJson(transactions).toByteArray())
            }
            Toast.makeText(this, "Backup saved to ${file.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importData() {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val files = downloadsDir.listFiles { _, name ->
                name.startsWith("finance_backup_") && name.endsWith(".json")
            }?.sortedByDescending { it.lastModified() }

            if (files.isNullOrEmpty()) {
                Toast.makeText(this, "No backup files found", Toast.LENGTH_SHORT).show()
                return
            }

            val fileNames = files.map { it.name }
            AlertDialog.Builder(this)
                .setTitle("Select Backup File")
                .setItems(fileNames.toTypedArray()) { _, which ->
                    FileInputStream(files[which]).use { stream ->
                        val json = stream.bufferedReader().use { it.readText() }
                        val type = object : TypeToken<List<Transaction>>() {}.type
                        transactions.clear()
                        transactions.addAll(Gson().fromJson(json, type))
                        saveData()
                        categoryAdapter.notifyDataSetChanged()
                        updateAllViews()
                        Toast.makeText(this, "Data restored successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val budgetChannel = NotificationChannel(
                BUDGET_CHANNEL_ID,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when you're close to or exceed your budget"
            }

            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to record your expenses"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(budgetChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    private fun showBudgetNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(this, BUDGET_CHANNEL_ID)
            .setSmallIcon(R.drawable.warnimg)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
            return
        }
        NotificationManagerCompat.from(this).notify(BUDGET_NOTIFICATION_ID, notification)
    }

    private fun scheduleDailyReminder() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)

            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
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

    // Add this method to check for today's transactions
    private fun checkDailyTransactions() {
        val today = dateFormat.format(Date())
        val hasTransactionToday = transactions.any { it.date == today }
        
        if (!hasTransactionToday) {
            // Check if we already reminded today
            val lastCheck = prefs.getString(LAST_TRANSACTION_CHECK_KEY, "") ?: ""
            if (lastCheck != today) {
                showDailyTransactionReminder()
                // Save that we reminded today
                prefs.edit().putString(LAST_TRANSACTION_CHECK_KEY, today).apply()
            }
        }
    }

    private fun showDailyTransactionReminder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DAILY_REMINDER_CHANNEL_ID,
                "Daily Transaction Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to add transactions daily"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, DAILY_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.cutlery)
            .setContentTitle("Daily Transaction Reminder")
            .setContentText("Don't forget to add today's transactions!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
            return
        }
        NotificationManagerCompat.from(this).notify(DAILY_REMINDER_NOTIFICATION_ID, notification)
    }

    // Add this method to schedule periodic checks
    private fun scheduleDailyTransactionCheck() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            action = "CHECK_DAILY_TRANSACTIONS"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set alarm for 8 PM every day
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)

            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun showReminderDialog() {
        val isReminderEnabled = prefs.getBoolean(REMINDER_ENABLED_KEY, false)
        
        AlertDialog.Builder(this)
            .setTitle("Daily Transaction Reminder")
            .setMessage(if (isReminderEnabled) 
                "Do you want to disable daily transaction reminders?" 
                else 
                "Do you want to enable daily transaction reminders?")
            .setPositiveButton(if (isReminderEnabled) "Disable" else "Enable") { _, _ ->
                val btnReminder = findViewById<ImageView>(R.id.btnReminder)
                if (isReminderEnabled) {
                    // Disable reminder
                    prefs.edit().putBoolean(REMINDER_ENABLED_KEY, false).apply()
                    btnReminder.setColorFilter(
                        ContextCompat.getColor(this, R.color.secondary_text)
                    )
                    // Cancel existing alarms
                    cancelDailyReminders()
                    Toast.makeText(this, "Daily reminders disabled", Toast.LENGTH_SHORT).show()
                } else {
                    // Enable reminder
                    prefs.edit().putBoolean(REMINDER_ENABLED_KEY, true).apply()
                    btnReminder.setColorFilter(
                        ContextCompat.getColor(this, R.color.primary_purple)
                    )
                    // Schedule reminders
                    scheduleDailyReminder()
                    scheduleDailyTransactionCheck()
                    Toast.makeText(this, "Daily reminders enabled", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun cancelDailyReminders() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        
        // Cancel transaction check alarm
        val checkIntent = Intent(this, ReminderReceiver::class.java).apply {
            action = "CHECK_DAILY_TRANSACTIONS"
        }
        val checkPendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            checkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(checkPendingIntent)

        // Cancel default reminder alarm
        val reminderIntent = Intent(this, ReminderReceiver::class.java)
        val reminderPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(reminderPendingIntent)
    }
}