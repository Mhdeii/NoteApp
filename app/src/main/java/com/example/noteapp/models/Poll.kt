package com.example.noteapp.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName


@Entity(tableName = "polls")
data class Poll(
    @PrimaryKey(autoGenerate = false)
    @SerializedName("id")
    val id: String,

    @SerializedName("text")
    val text: String,

    @SerializedName("options")
    val options: List<String>,

    @SerializedName("authorId")
    val authorId: String,

    @SerializedName("dateCreated")
    val dateCreated: String,

    var votes: MutableList<Vote> = mutableListOf()
)

@Entity(tableName = "votes")
data class Vote(
    @SerializedName("pollId")
    val pollId: String,

    @SerializedName("voterId")
    val voterId: String,

    @SerializedName("option")
    var option: String
)


