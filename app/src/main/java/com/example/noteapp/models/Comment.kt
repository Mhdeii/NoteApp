package com.example.noteapp.models

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey
    @SerializedName("commentId")
    var commentId: String = UUID.randomUUID().toString(),

    @SerializedName("postId")
    var postId: String = "",

    @SerializedName("userId")
    var userId: Int = -1,

    @SerializedName("text")
    var text: String = "",

    @SerializedName("date")
    var date: String = "",

    @SerializedName("time")
    var time: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: UUID.randomUUID().toString(),
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(commentId)
        parcel.writeString(postId)
        parcel.writeInt(userId)
        parcel.writeString(text)
        parcel.writeString(date)
        parcel.writeString(time)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Comment> {
        override fun createFromParcel(parcel: Parcel): Comment {
            return Comment(parcel)
        }

        override fun newArray(size: Int): Array<Comment?> {
            return arrayOfNulls(size)
        }
    }
}
