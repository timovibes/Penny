package com.example.penny.data.model

data class Budget(
    val id: String = "",
    val category: String = "",
    val monthlyLimit: Double = 0.0,
    val year: Int = 0,
    val month: Int = 0 // 1–12, matches Transaction's month convention
)