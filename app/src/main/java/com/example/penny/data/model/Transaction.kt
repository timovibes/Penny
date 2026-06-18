package com.example.penny.data.model

import com.google.firebase.Timestamp

data class Transaction(
    val id : String = "",
    val amount : Double = 0.0,
    val type : String = "",
    val category : String = "",
    val note : String = "",
    val date : Timestamp = Timestamp.now(),
    val currency: String = "KES"
)