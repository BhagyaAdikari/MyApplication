package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button

class OnBoardingActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_on_boarding2)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btn_next)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Handle "Next" button click
        val btnNext = findViewById<Button>(R.id.btn_next)
        btnNext.setOnClickListener {
            val intent = Intent(this, OnBoardingActivity3::class.java)
            startActivity(intent)
            finish() // Optional: Close current activity to prevent going back
        }
    }
}