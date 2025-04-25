package com.anush.vibora.Activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.anush.vibora.databinding.ActivityMainLoginBinding

class MainLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}