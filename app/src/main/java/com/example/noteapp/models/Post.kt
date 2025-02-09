package com.example.noteapp.models

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    var id: String = UUID.randomUUID().toString(),

    @SerializedName("title")
    var title: String = "",

    @SerializedName("authorId")
    var authorId: Int = -1,

    @SerializedName("description")
    var description: String = "",

    @SerializedName("views")
    var views: Int = 0,

    @SerializedName("date")
    var date: String = "",

    @SerializedName("time")
    var time: String = "",

    @SerializedName("comments")
    var comments: List<Comment> = emptyList(),

    @SerializedName("photo")
    var img: String? = null,

    @SerializedName("latitude")
    var latitude: Double = 0.0,

    @SerializedName("longitude")
    var longitude: Double = 0.0
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readInt().toString(),
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.createTypedArrayList(Comment)!!,
        parcel.readString(),
        parcel.readDouble(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id.toInt())
        parcel.writeString(title)
        parcel.writeInt(authorId)
        parcel.writeString(description)
        parcel.writeInt(views)
        parcel.writeString(date)
        parcel.writeString(time)
        parcel.writeTypedList(comments)
        parcel.writeString(img)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Post> {
        override fun createFromParcel(parcel: Parcel): Post {
            return Post(parcel)
        }

        override fun newArray(size: Int): Array<Post?> {
            return arrayOfNulls(size)
        }
    }
}
