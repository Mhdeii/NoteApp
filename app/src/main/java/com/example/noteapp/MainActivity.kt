package com.example.noteapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.ezxample.noteapp.R
import com.ezxample.noteapp.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var fragmentManager: FragmentManager
    private lateinit var binding: ActivityMainBinding

    private var username: String? = null
    private var name: String? = null
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve username, name, and userId from intent extras
        username = intent.getStringExtra("USERNAME")
        name = intent.getStringExtra("NAME")
        userId = intent.getIntExtra("USER_ID", -1)

        // Set username and name in navigation header
        val navigationView = binding.navigationView
        val headerView = navigationView.getHeaderView(0)
        val textViewName: TextView = headerView.findViewById(R.id.textViewUsername)

        textViewName.text = name

        // Initialize HomeFragment with username and userId
        val homeFragment = HomeFragment().apply {
            arguments = Bundle().apply {
                putString("USERNAME", username)
                putInt("USER_ID", userId)
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, homeFragment)
            .commit()

        setSupportActionBar(binding.toolbar)
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerlayout,
            binding.toolbar,
            R.string.nav_open,
            R.string.nav_close
        )
        binding.drawerlayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener(this)

        binding.bottomNavigationView.background = null
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.button_map -> {
                    openFragment(MapFragment())
                }
                R.id.poll -> {
                    openFragment(PollFragment())
                }
                R.id.button_home -> {
                    openFragment(HomeFragment())
                }
            }
            true
        }

        fragmentManager = supportFragmentManager
        if (savedInstanceState == null) {
            // Only open HomeFragment initially if no saved instance state (first launch or fresh start)
            openFragment(HomeFragment())
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation drawer item clicks
        when (item.itemId) {
            R.id.settings -> {
                openFragment(SettingsFragment())
            }
            R.id.notifications -> {
                openFragment(NotificationFragment())
            }
            R.id.logout -> {
                // Clear session data
                val sharedPreferences = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.clear()
                editor.apply()

                // Navigate to LoginActivity
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show()
                startActivity(intent)
                finish()
            }
        }

        // Close navigation drawer after item selection
        binding.drawerlayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (binding.drawerlayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerlayout.closeDrawer(GravityCompat.START)
        } else {
            // If on HomeFragment, close the app normally
            if (fragmentManager.backStackEntryCount == 0) {
                if (doubleBackToExitPressedOnce) {
                    moveTaskToBack(true)
                    return
                }

                this.doubleBackToExitPressedOnce = true
                Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()

                Handler(Looper.getMainLooper()).postDelayed({
                    doubleBackToExitPressedOnce = false
                }, 3000) // Adjust the delay time as needed
            } else {
                super.onBackPressed()
            }
        }
    }

    private var doubleBackToExitPressedOnce = false

    private fun openFragment(fragment: Fragment) {
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, fragment)
        fragmentTransaction.commit()
    }
}