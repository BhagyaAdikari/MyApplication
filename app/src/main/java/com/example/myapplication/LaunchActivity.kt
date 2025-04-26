package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.progressindicator.CircularProgressIndicator

class LaunchActivity : AppCompatActivity() {

    private lateinit var lottieAnimation: LottieAnimationView
    private lateinit var progressBar: CircularProgressIndicator
    private val SPLASH_DELAY = 5000L // 2.5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        enableEdgeToEdge()
        initializeViews()
        setupWindowInsets()
        setupAnimation()
        scheduleNavigation()
    }

    private fun initializeViews() {
        lottieAnimation = findViewById(R.id.lottieAnimation)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupAnimation() {
        lottieAnimation.apply {
            setAnimation(R.raw.money)
            speed = 0.5f
            playAnimation()
        }
    }

    private fun scheduleNavigation() {
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToOnboarding()
        }, SPLASH_DELAY)
    }

    private fun navigateToOnboarding() {
        val intent = Intent(this, OnBoardingActivity1::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        lottieAnimation.cancelAnimation()
        super.onDestroy()
    }
}