package com.example.noteapp

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ezxample.noteapp.R
import com.ezxample.noteapp.databinding.ItemChatMessageBinding
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale


class ChatAdapter(private val messages: List<Message>, private val currentUserId: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_DATE = 0
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.formattedDate != null -> VIEW_TYPE_DATE
            message.senderId.toInt() == currentUserId -> VIEW_TYPE_SENT
            else -> VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DATE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date_highlight, parent, false)
                DateViewHolder(view)
            }
            VIEW_TYPE_SENT -> {
                val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SentViewHolder(binding)
            }
            VIEW_TYPE_RECEIVED -> {
                val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ReceivedViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is DateViewHolder -> holder.bind(message)
            is SentViewHolder -> holder.bind(message)
            is ReceivedViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    class DateViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val dateTextView: TextView = view.findViewById(R.id.dateTextView)
        fun bind(message: Message) {
            dateTextView.text = message.formattedDate
        }
    }

    class SentViewHolder(private val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.sentMessageLayout.visibility = View.VISIBLE
            binding.receivedMessageLayout.visibility = View.GONE
            binding.sentMessageTextView.text = message.text
            binding.sentMessageTimeTextView.text = formatTimestamp(message.timestamp)
            binding.sentMessageTimeTextView.visibility = View.VISIBLE

            if (message.text.length > 300) {
                val spannableString = SpannableString(message.text.take(300) + "  ... Read More")
                val readMoreColor = ContextCompat.getColor(binding.root.context, R.color.read_more_color_for_sent)
                spannableString.setSpan(ForegroundColorSpan(readMoreColor), 300, spannableString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                binding.sentMessageTextView.text = spannableString
                binding.sentMessageTextView.setOnClickListener {
                    binding.sentMessageTextView.text = message.text
                }
            } else {
                binding.sentMessageTextView.text = message.text
                binding.sentMessageTextView.setOnClickListener(null)
            }
        }

        private fun formatTimestamp(timestamp: Timestamp): String {
            val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return dateFormat.format(timestamp.toDate())
        }
    }




    class ReceivedViewHolder(private val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.sentMessageLayout.visibility = View.GONE
            binding.receivedMessageLayout.visibility = View.VISIBLE
            binding.receivedMessageTextView.text = message.text
            binding.receivedMessageTimeTextView.text = formatTimestamp(message.timestamp)
            binding.receivedMessageTimeTextView.visibility = View.VISIBLE

            if (message.text.length > 300) {
                val spannableString = SpannableString(message.text.take(300) + "  ... Read More")
                val readMoreColor = ContextCompat.getColor(binding.root.context, R.color.read_more_color_for_received)
                spannableString.setSpan(ForegroundColorSpan(readMoreColor), 300, spannableString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                binding.receivedMessageTextView.text = spannableString
                binding.receivedMessageTextView.setOnClickListener {
                    binding.receivedMessageTextView.text = message.text
                }
            } else {
                binding.receivedMessageTextView.text = message.text
                binding.receivedMessageTextView.setOnClickListener(null)
            }
        }

        private fun formatTimestamp(timestamp: Timestamp): String {
            val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return dateFormat.format(timestamp.toDate())
        }
    }





}

