package com.example.noteapp

import com.example.noteapp.models.Poll
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.ezxample.noteapp.R
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PollFragment : Fragment() {
    private lateinit var pollDao: PollDao
    private lateinit var voteDao: VoteDao
    private lateinit var pollApiService: ApiService
    private lateinit var expandableListView: ExpandableListView
    private lateinit var pollList: List<Poll>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_poll, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pollApiService = RetrofitClient.getClient().create(ApiService::class.java)

        val db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java, "app_database"
        ).build()
        pollDao = db.pollDao()
        voteDao = db.voteDao()

        expandableListView = view.findViewById(R.id.expandableListView)

        if (::pollList.isInitialized.not()) {
            fetchPollData()
        }
    }

    private fun fetchPollData() {
        lifecycleScope.launch {
            val pollsFromServer = withContext(Dispatchers.IO) {
                pollApiService.getPolls()
            }

            val pollEntities = pollsFromServer.map {
                PollEntity(
                    id = it.id,
                    text = it.text,
                    options = Gson().toJson(it.options),
                    authorId = it.authorId,
                    dateCreated = it.dateCreated,
                    votes = Gson().toJson(it.votes)
                )
            }

            withContext(Dispatchers.IO) {
                pollDao.insertPolls(pollEntities)
            }

            pollList = pollEntities.map {
                it.toPoll()
            }

            setupExpandableListView()
        }
    }

    private fun setupExpandableListView() {
        val currentUserId = getCurrentUserId()
        val adapter = PollExpandableListAdapter(
            requireContext(),
            pollList,
            currentUserId,
            pollApiService,
            lifecycleScope
        )
        expandableListView.setAdapter(adapter)
    }


    private fun getCurrentUserId(): Int {
        val sharedPreferences = requireContext().getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("userId", -1)
    }
}