package com.example.noteapp

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ezxample.noteapp.databinding.ActivityChatBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var messages: MutableList<Message>
    private var lastMessageId: Long = 0
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = intent.getLongExtra("USER_ID", -1L)
        val username = intent.getStringExtra("USERNAME") ?: ""
        val currentUserId = getCurrentUserId()

        Log.d("ChatActivity", "Receiver ID: $userId")

        supportActionBar?.title = username
        binding.NameTextView.text = username

        messages = mutableListOf()
        layoutManager = LinearLayoutManager(this)
        chatAdapter = ChatAdapter(messages, currentUserId.toInt())
        binding.recyclerView.apply {
            layoutManager = this@ChatActivity.layoutManager
            adapter = chatAdapter
        }

        firestore = FirebaseFirestore.getInstance()
        val chatId = if (userId < currentUserId) "${userId}_${currentUserId}" else "${currentUserId}_${userId}"
        val chatRef = firestore.collection("chats").document(chatId).collection("messages")

        receiveMessages(chatRef)

        binding.sendButton.setOnClickListener {
            val text = binding.messageEditText.text.toString()
            if (text.isNotEmpty()) {
                sendMessage(chatRef, text, currentUserId, userId)
                binding.messageEditText.text?.clear()
                scrollToBottom()
            }
        }

        binding.root.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val rect = Rect()
                binding.root.getWindowVisibleDisplayFrame(rect)
                val screenHeight = binding.root.rootView.height
                val keypadHeight = screenHeight - rect.bottom
                if (keypadHeight > screenHeight * 0.15) {
                    if (layoutManager.findLastCompletelyVisibleItemPosition() == messages.size - 1) {
                        binding.recyclerView.post {
                            binding.recyclerView.scrollToPosition(messages.size - 1)
                        }
                    }
                }
            }
        })

        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    private fun getCurrentUserId(): Long {
        val sharedPreferences = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        return sharedPreferences.getInt("currentUserId", -1).toLong()
    }

    private fun sendMessage(chatRef: CollectionReference, text: String, senderId: Long, receiverId: Long) {
        lastMessageId++

        val message = Message(
            id = lastMessageId,
            senderId = senderId,
            receiverId = receiverId,
            text = text,
            timestamp = Timestamp.now()
        )
        chatRef.add(message).addOnFailureListener { e ->
            e.printStackTrace()
        }
    }

    private fun receiveMessages(chatRef: CollectionReference) {
        chatRef.orderBy("timestamp", Query.Direction.ASCENDING).addSnapshotListener { snapshots, e ->
            if (e != null) {
                e.printStackTrace()
                return@addSnapshotListener
            }

            messages.clear()
            var previousDate: String? = null
            for (doc in snapshots!!) {
                val message = doc.toObject(Message::class.java)
                val currentDate = formatDate(message.timestamp)
                if (currentDate != previousDate) {
                    messages.add(Message(formattedDate = currentDate))
                    previousDate = currentDate
                }
                messages.add(message)
            }
            chatAdapter.notifyDataSetChanged()
            scrollToBottom()
        }
    }

    private fun formatDate(timestamp: Timestamp): String {
        val date = timestamp.toDate()
        val today = Calendar.getInstance().apply { time = Date() }
        val messageDate = Calendar.getInstance().apply { time = date }

        return when {
            today[Calendar.YEAR] == messageDate[Calendar.YEAR] &&
                    today[Calendar.DAY_OF_YEAR] == messageDate[Calendar.DAY_OF_YEAR] -> "Today"

            today[Calendar.YEAR] == messageDate[Calendar.YEAR] &&
                    today[Calendar.DAY_OF_YEAR] - 1 == messageDate[Calendar.DAY_OF_YEAR] -> "Yesterday"

            else -> {
                val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                dateFormat.format(date)
            }
        }
    }

    private fun scrollToBottom() {
        binding.recyclerView.post {
            binding.recyclerView.scrollToPosition(messages.size - 1)
        }
    }
}