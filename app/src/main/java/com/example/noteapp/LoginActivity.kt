package com.example.noteapp

import com.ezxample.noteapp.R
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.noteapp.models.Profile
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.BigInteger
import java.security.MessageDigest



class LoginActivity : AppCompatActivity() {
    private lateinit var apiService: ApiService
    private lateinit var userEdit: EditText
    private lateinit var passEdit: EditText
    private lateinit var loginButton: Button
    private lateinit var googleSignInButton: Button
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    private val REQ_ONE_TAP = 100

    private var name: String? = null
    private var username: String? = null
    private var password: String? = null

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        checkSession()

        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setPasswordRequestOptions(BeginSignInRequest.PasswordRequestOptions.builder()
                .setSupported(true)
                .build())
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId("108756941281-af8iura8r5tf2f2d3593vvk13ghsf5a6.apps.googleusercontent.com")
                    .setFilterByAuthorizedAccounts(false)
                    .build())
            .setAutoSelectEnabled(true)
            .build()

        initRetrofit()
        setupViews()
        setupListeners()
        setupGoogleSignIn()
    }

    private fun checkSession() {
        val sharedPreferences = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        val loginTime = sharedPreferences.getLong("loginTime", 0)
        val currentTime = System.currentTimeMillis()
        val twoHoursInMillis = resources.getInteger(R.integer.two_hrs)

        if (isLoggedIn && currentTime - loginTime <= twoHoursInMillis) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            val editor = sharedPreferences.edit()
            editor.clear()
            editor.apply()
        }
    }

    private fun initRetrofit() {
        val retrofit = RetrofitClient.getClient()
        Log.d(TAG, "Retrofit instance created: $retrofit")
        apiService = retrofit.create(ApiService::class.java)
    }

    private fun setupViews() {
        userEdit = findViewById(R.id.userEdit)
        passEdit = findViewById(R.id.passEdit)
        loginButton = findViewById(R.id.signupbutton)
        googleSignInButton = findViewById(R.id.google_sign_in_button)
    }

    private fun setupListeners() {
        loginButton.setOnClickListener {
            username = userEdit.text.toString()
            password = userEdit.text.toString().toMD5()

            Log.d("LoginDebug", "Entered username: $username")
            Log.d("LoginDebug", "Entered password hash: $password")

            authenticateUser(username!!, password!!)
        }

        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signInWithGoogle() {
        val signInIntent: Intent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_ONE_TAP -> {
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(data)
                    val idToken = credential.googleIdToken
                    username = credential.id
                    password = credential.password

                    when {
                        idToken != null -> {
                            Log.d(TAG, "Got ID token.")
                        }
                        password != null -> {
                            Log.d(TAG, "Got password.")
                        }
                        else -> {
                            Log.d(TAG, "No ID token or password!")
                        }
                    }
                } catch (e: ApiException) {
                    Log.e(TAG, "One Tap sign-in failed: ${e.localizedMessage}")
                }
            }
            RC_SIGN_IN -> {
                val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleSignInResult(task)
            }
        }
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            username = account.email
            name = account.displayName

            checkIfUserExists(username!!, name!!)
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign in failed", e)
            onLoginFailure()
        }
    }

    private fun checkIfUserExists(userName: String, name: String) {
        val call: Call<List<Profile>> = apiService.getProfiles()
        call.enqueue(object : Callback<List<Profile>> {
            override fun onResponse(call: Call<List<Profile>>, response: Response<List<Profile>>) {
                if (response.isSuccessful) {
                    val profiles: List<Profile>? = response.body()
                    profiles?.let {
                        val user = it.find { profile ->
                            profile.userName == userName
                        }
                        if (user != null) {
                            // User exists, proceed to main activity
                            onLoginSuccess(user.id.toInt())
                        } else {
                            // User doesn't exist, create new profile with a generated password
                            val generatedPassword = generateRandomPassword()
                            createNewProfile(name, userName, generatedPassword)
                        }
                    }
                } else {
                    showToast("Authentication failed. Error code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Profile>>, t: Throwable) {
                showToast("Authentication failed. Please check your internet connection and try again.")
            }
        })
    }

    private fun generateRandomPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+<>?"
        return (1..12)
            .map { chars.random() }
            .joinToString("")
    }

    private fun createNewProfile(name: String, userName: String, password: String) {
        val profile = Profile(name = name, userName = userName, password = password)
        val addProfileCall: Call<Profile> = apiService.addProfile(profile)
        addProfileCall.enqueue(object : Callback<Profile> {
            override fun onResponse(call: Call<Profile>, response: Response<Profile>) {
                if (response.isSuccessful) {
                    this@LoginActivity.name = name
                    this@LoginActivity.username = userName
                    this@LoginActivity.password = password
                    onLoginSuccess(response.body()?.id?.toInt() ?: 0)
                } else {
                    showToast("Failed to create profile. Error code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Profile>, t: Throwable) {
                showToast("Failed to create profile. Please check your internet connection and try again.")
            }
        })
    }
    private fun authenticateUser(username: String, password: String) {
        val call: Call<List<Profile>> = apiService.getProfiles()
        call.enqueue(object : Callback<List<Profile>> {
            override fun onResponse(call: Call<List<Profile>>, response: Response<List<Profile>>) {
                Log.d(TAG, "API Response: ${response.raw()}")
                if (response.isSuccessful) {
                    val profiles: List<Profile>? = response.body()
                    profiles?.let {
                        val user = it.find { profile ->
                            profile.userName.equals(username, ignoreCase = true) && profile.password == password
                        }
                        if (user != null) {
                            this@LoginActivity.name = user.name
                            this@LoginActivity.username = user.userName
                            this@LoginActivity.password = user.password
                            onLoginSuccess(user.id.toInt())
                        } else {
                            showToast("Invalid username or password")
                        }
                    }
                } else {
                    showToast("Authentication failed. Error code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Profile>>, t: Throwable) {
                Log.e(TAG, "API call failed", t)
                showToast("Authentication failed. Please check your internet connection and try again.")
            }
        })
    }

    private fun onLoginSuccess(userId: Int) {
        val sharedPreferences = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putLong("loginTime", System.currentTimeMillis())
        editor.putBoolean("isLoggedIn", true)
        editor.putInt("userId", userId)
        editor.putString("USERNAME", username)
        editor.putString("NAME", name)
        editor.putString("PASSWORD", password)
        editor.apply()

        val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
            putExtra("USERNAME", username)
            putExtra("USER_ID", userId)
        }
        startActivity(intent)
        finish()
    }




    private fun onLoginFailure() {
        showToast("Google Sign-In Failed")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun String.toMD5(): String {
        val md = MessageDigest.getInstance("MD5")
        val byteArray = md.digest(this.toByteArray())
        val bigInteger = BigInteger(1, byteArray)
        var hashText = bigInteger.toString(16)
        while (hashText.length < 32) {
            hashText = "0$hashText"
        }
        return hashText
    }
}
