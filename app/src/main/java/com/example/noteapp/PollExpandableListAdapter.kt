package com.example.noteapp

import com.example.noteapp.models.Poll
import com.example.noteapp.models.Vote
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.ezxample.noteapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PollExpandableListAdapter(
    private val context: Context,
    private val pollList: List<Poll>,
    private val currentUserId: Int,
    private val apiService: ApiService,
    private val scope: CoroutineScope
) : BaseExpandableListAdapter() {

    private var allVotes: List<Vote> = emptyList()

    init {
        scope.launch {
            allVotes = apiService.getVotes()
        }
    }

    override fun getGroupCount(): Int {
        return pollList.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return 1
    }

    override fun getGroup(groupPosition: Int): Any {
        return pollList[groupPosition]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return pollList[groupPosition]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        val group = getGroup(groupPosition) as Poll
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val itemView = convertView ?: inflater.inflate(R.layout.poll_group, parent, false)
        val textViewGroup = itemView.findViewById<TextView>(R.id.poll_question)
        textViewGroup.text = group.text
        return itemView
    }

    override fun getChildView(
        groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?
    ): View {
        val poll = getGroup(groupPosition) as Poll
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val itemView = convertView ?: inflater.inflate(R.layout.poll_question_item, parent, false)
        val radioGroup = itemView.findViewById<RadioGroup>(R.id.radioGroupOptions)

        radioGroup.removeAllViews()

        poll.options.forEachIndexed { index, option ->
            val radioButton = RadioButton(context)
            radioButton.id = index
            radioButton.text = option
            radioGroup.addView(radioButton)
            radioButton.isChecked = allVotes.any { it.voterId == currentUserId.toString() && it.pollId == poll.id && it.option == option }
        }

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedOption = poll.options[checkedId]
            val vote = Vote(pollId = poll.id, voterId = currentUserId.toString(), option = selectedOption)
            scope.launch {
                updateVoteOnServer(poll.id, currentUserId.toString(), vote) // Pass correct parameters here
            }
        }

        return itemView
    }


    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }


    private suspend fun updateVoteOnServer(pollId: String, voterId: String, vote: Vote) {
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateVotes(pollId, voterId, vote)
                if (response.isSuccessful) {
                    Log.d("PollAdapter", "Vote updated successfully on server")
                } else {
                    Log.e("PollAdapter", "Failed to update vote on server: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("PollAdapter", "Error updating vote on server", e)
            }
        }
    }

}