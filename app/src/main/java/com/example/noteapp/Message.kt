package com.example.noteapp

import com.google.firebase.Timestamp

data class Message(
    val id: Long = 0,
    val senderId: Long = 0,
    val receiverId: Long = 0,
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    var formattedDate: String? = null
)