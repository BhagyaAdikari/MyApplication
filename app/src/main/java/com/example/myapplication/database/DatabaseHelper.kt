package com.example.myapplication.database

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.myapplication.models.Transaction
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "finance.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_TRANSACTIONS = "transactions"
        
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_AMOUNT = "amount"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_CATEGORY = "category"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_DESCRIPTION = "description"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_TRANSACTIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_AMOUNT REAL NOT NULL,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_CATEGORY TEXT NOT NULL,
                $COLUMN_DATE TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT
            )
        """.trimIndent()
        
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSACTIONS")
        onCreate(db)
    }

    fun getAllTransactions(): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val db = this.readableDatabase
        val cursor = db.query(TABLE_TRANSACTIONS, null, null, null, null, null, "$COLUMN_DATE DESC")

        with(cursor) {
            while (moveToNext()) {
                val id = getInt(getColumnIndexOrThrow(COLUMN_ID))
                val title = getString(getColumnIndexOrThrow(COLUMN_TITLE))
                val amount = getDouble(getColumnIndexOrThrow(COLUMN_AMOUNT))
                val type = getString(getColumnIndexOrThrow(COLUMN_TYPE))
                val category = getString(getColumnIndexOrThrow(COLUMN_CATEGORY))
                val dateStr = getString(getColumnIndexOrThrow(COLUMN_DATE))
                val description = getString(getColumnIndexOrThrow(COLUMN_DESCRIPTION))

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = dateFormat.parse(dateStr) ?: Date()

                transactions.add(Transaction(id, title, amount, type, category, date, description))
            }
        }
        cursor.close()
        return transactions
    }

    fun updateTransaction(transaction: Transaction) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, transaction.title)
            put(COLUMN_AMOUNT, transaction.amount)
            put(COLUMN_TYPE, transaction.type)
            put(COLUMN_CATEGORY, transaction.category)
            put(COLUMN_DATE, SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(transaction.date))
            put(COLUMN_DESCRIPTION, transaction.description)
        }
        
        db.update(
            TABLE_TRANSACTIONS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(transaction.id.toString())
        )
    }

    fun deleteTransaction(id: Int) {
        val db = this.writableDatabase
        db.delete(
            TABLE_TRANSACTIONS,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        )
    }
} 