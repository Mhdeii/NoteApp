package com.example.noteapp.models
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

data class Profile(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    var id: String = "",

    @SerializedName("name")
    var name: String,

    @SerializedName("username")
    var userName: String,

    @SerializedName("password")
    var password: String
)
