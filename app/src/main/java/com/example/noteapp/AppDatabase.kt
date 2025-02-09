package com.example.noteapp

import com.example.noteapp.models.Poll
import com.example.noteapp.models.Vote
import android.content.Context
import androidx.room.*
import com.example.noteapp.models.Comment
import com.example.noteapp.models.Post
import com.google.gson.Gson

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val authorId: Int,
    val views: Int,
    val date: String,
    val time: String,
    val img: String,
    val latitude: Double,
    val longitude: Double
) {
    fun toPost(): Post {
        return Post(
            id = this.id,
            title = this.title,
            description = this.description,
            authorId = this.authorId,
            views = this.views,
            date = this.date,
            time = this.time,
            img = this.img,
            latitude = this.latitude,
            longitude = this.longitude,
            comments = emptyList()
        )
    }
}

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val commentId: String,
    val postId: String,
    val userId: Int,
    val text: String,
    val date: String,
    val time: String
) {
    fun toComment(): Comment {
        return Comment(
            commentId = this.commentId,
            postId = this.postId,
            userId = this.userId,
            text = this.text,
            date = this.date,
            time = this.time
        )
    }
}

@Entity(tableName = "polls")
data class PollEntity(
    @PrimaryKey val id: String,
    val text: String,
    val options: String, // Store options as a JSON string
    val authorId: String,
    val dateCreated: String,
    val votes: String // Store votes as a JSON string
) {
    fun toPoll(): Poll {
        val optionsList = Gson().fromJson(options, Array<String>::class.java)?.toList() ?: emptyList()
        val votesList = Gson().fromJson(votes, Array<Vote>::class.java)?.toList() ?: emptyList()

        return Poll(
            id = this.id,
            text = this.text,
            options = optionsList,
            authorId = this.authorId,
            dateCreated = this.dateCreated,
            votes = votesList.toMutableList()
        )
    }
}

@Entity(tableName = "votes")
data class VoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val pollId: String,
    val voterId: String,
    var option: String
)

    @Dao
    interface VoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVotes(votes: List<VoteEntity>)

    @Query("SELECT * FROM votes WHERE pollId = :pollId")
    suspend fun getVotesByPollId(pollId: String): List<VoteEntity>

    @Query("DELETE FROM votes WHERE pollId = :pollId AND voterId = :voterId")
    suspend fun deleteVote(pollId: String, voterId: String)

    @Query("DELETE FROM votes")
    suspend fun deleteAllVotes()
    }

    @Dao
    interface PollDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertPolls(polls: List<PollEntity>)

        @Query("SELECT * FROM polls")
        suspend fun getAllPolls(): List<PollEntity>

        @Query("DELETE FROM polls WHERE id = :pollId")
        suspend fun deletePoll(pollId: String)

        @Update
        suspend fun updatePoll(poll: PollEntity)

        @Query("UPDATE polls SET votes = :votes WHERE id = :pollId")
        suspend fun updatePollVotes(pollId: String, votes: String)

        @Query("DELETE FROM polls")
        suspend fun deleteAllPolls()
    }

    @Dao
    interface CommentDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertComments(comments: List<CommentEntity>)

        @Query("SELECT * FROM comments WHERE postId = :postId")
        suspend fun getCommentsByPostId(postId: String): List<CommentEntity>

        @Query("DELETE FROM comments WHERE commentId = :commentId")
        suspend fun deleteComment(commentId: String)

        @Update
        suspend fun updateComment(comment: CommentEntity)

        @Query("DELETE FROM comments")
        suspend fun deleteAllComments()
    }

    @Dao
    interface PostDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertPosts(posts: List<PostEntity>)

        @Query("SELECT * FROM posts")
        suspend fun getAllPosts(): List<PostEntity>

        @Query("DELETE FROM posts WHERE id = :postId")
        suspend fun deletePost(postId: String)

        @Update
        suspend fun updatePost(post: PostEntity)

        @Query("SELECT * FROM posts ORDER BY time ASC")
        fun getAllPostsSortedByTime(): List<PostEntity>

        @Query("SELECT * FROM posts ORDER BY date ASC")
        fun getAllPostsSortedByDate(): List<PostEntity>

        @Query("DELETE FROM posts")
        suspend fun deleteAllPosts()
    }

    @Database(entities = [PostEntity::class, PollEntity::class, CommentEntity::class, VoteEntity::class], version = 1, exportSchema = false)
    abstract class AppDatabase : RoomDatabase() {
        abstract fun postDao(): PostDao
        abstract fun pollDao(): PollDao
        abstract fun commentDao(): CommentDao
        abstract fun voteDao(): VoteDao

        companion object {
            @Volatile
            private var INSTANCE: AppDatabase? = null

            fun getDatabase(context: Context): AppDatabase {
                return INSTANCE ?: synchronized(this) {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "app_database"
                    ).fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                    instance
                }
            }
        }
    }

