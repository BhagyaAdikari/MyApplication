package com.example.myapplication.models

import java.util.Date

data class Transaction(
    val id: Int,
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val date: Date,
    val description: String?
) 