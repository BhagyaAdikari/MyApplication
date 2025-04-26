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
    private lateinit var rvTransactions: RecyclerView
    private lateinit var summaryContainer: LinearLayout
    private lateinit var tvBudgetAmount: TextView
    private lateinit var progressBudget: ProgressBar
    private lateinit var tvBudgetStatus: TextView
    private lateinit var currencySpinner: Spinner
    private lateinit var btnBackup: MaterialCardView
    private lateinit var btnAddTransaction: MaterialCardView
    private lateinit var btnSetBudget: MaterialButton

    // Data
    private val transactions = mutableListOf<Transaction>()
    private var monthlyBudget: Double = 0.0
    private var currency: String = "$"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private lateinit var transactionAdapter: TransactionAdapter

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        prefs = getSharedPreferences("finance_prefs", MODE_PRIVATE)
        setupRecyclerView()
        loadData()
        createNotificationChannels()
        setClickListeners()
        checkBudgetAlerts()
        scheduleDailyReminder()
    }

    private fun initViews() {
        rvTransactions = findViewById(R.id.rvTransactions)
        summaryContainer = findViewById(R.id.summaryContainer)
        tvBudgetAmount = findViewById(R.id.tvBudgetAmount)
        progressBudget = findViewById(R.id.progressBudget)
        tvBudgetStatus = findViewById(R.id.tvBudgetStatus)
        currencySpinner = findViewById(R.id.spCurrency)
        btnBackup = findViewById(R.id.btnBackup)
        btnAddTransaction = findViewById(R.id.btnAddTransaction)
        btnSetBudget = findViewById(R.id.btnSetBudget)
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
        transactionAdapter = TransactionAdapter(transactions,
            onItemClick = { showTransactionOptions(it) },
            onItemLongClick = { showTransactionOptions(it) }
        )

        transactionAdapter.currency = currency
        rvTransactions.apply {
            adapter = transactionAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(
                DividerItemDecoration(
                    this@MainActivity,
                    LinearLayoutManager.VERTICAL
                )
            )
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                deleteTransaction(transactions[viewHolder.adapterPosition])
            }
        }).attachToRecyclerView(rvTransactions)
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

        currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currency = parent?.getItemAtPosition(position).toString().substringBefore(" ")
                transactionAdapter.currency = currency
                transactionAdapter.notifyDataSetChanged()
                saveData()
                updateAllViews()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateAllViews() {
        updateCategorySummary()
        updateBudgetViews()
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
        transactionAdapter.notifyDataSetChanged()
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
                        transactionAdapter.notifyItemInserted(0)
                        saveData()
                        updateAllViews()
                        rvTransactions.scrollToPosition(0)
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
            transactionAdapter.notifyItemRemoved(position)
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
                            transactionAdapter.notifyItemChanged(position)
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
            val files = filesDir.listFiles { _, name ->
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
                        transactionAdapter.notifyDataSetChanged()
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
}