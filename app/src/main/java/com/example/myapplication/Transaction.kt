package com.example.myapplication

import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val category: String,
    val date: String,
    val type: String // "Income" or "Expense"
)