package com.example.noteapp

import com.example.noteapp.models.Poll
import com.example.noteapp.models.Vote
import com.example.noteapp.models.Post
import com.example.noteapp.models.Profile
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("posts")
    fun getPosts(
        @Query("sort") sort: String,
        @Query("order") order: String
    ): Call<List<Post>>

    @GET("profile")
    fun getProfiles(): Call<List<Profile>>

    @DELETE("posts/{id}")
    fun deletePost(@Path("id") postId: String): Call<Void>

    @POST("profile")
    fun addProfile(@Body profile: Profile): Call<Profile>

    @PUT("posts/{id}")
    suspend fun updatePost(@Path("id") postId: String, @Body post: Post): Response<Post>

    @POST("posts")
    fun addPost(@Body post: Post): Call<Post>

    @GET("profile/{id}")
    fun getUsernameById(@Path("id") userId: Int): Call<Profile>

    @PUT("polls/{id}/votes")
    suspend fun updatePollVotes(@Path("id") pollId: String, @Body votes: List<Vote>): Response<Void>

    @GET("polls")
    suspend fun getPolls(): List<Poll>

    @GET("votes")
    suspend fun getVotes(): List<Vote>

    @PUT("votes/{pollId}/{voterId}")
    suspend fun updateVotes(
        @Path("pollId") pollId: String,
        @Path("voterId") voterId: String,
        @Body vote: Vote
    ): Response<Void>
}
