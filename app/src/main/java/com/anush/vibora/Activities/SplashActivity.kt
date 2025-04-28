package com.anush.vibora.Activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.anush.vibora.Helpers.SharedPrefs
import com.anush.vibora.Utils.Constants
import com.anush.vibora.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val splashTime: Long = 2000 // 2 seconds
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SharedPrefs.init(this)
        auth = FirebaseAuth.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginStatus()
        }, splashTime)
    }

    private fun checkLoginStatus() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            SharedPrefs.putBoolean(Constants.PREF_IS_LOGGED_IN, true)
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            SharedPrefs.putBoolean(Constants.PREF_IS_LOGGED_IN, false)
            startActivity(Intent(this, MainLoginActivity::class.java))
        }
        finish()
    }

}