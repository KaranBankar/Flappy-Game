package com.kbgaming.flappyball

import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SelectModesActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_select_modes)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = ContextCompat.getColor(this, R.color.selectmode)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Set light status bar icons (white battery, signal, etc.)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Fallback for API 21-22: Use a darker shade if light icons aren't supported
            window.statusBarColor = ContextCompat.getColor(this, R.color.selectmode)
        }

        // Initialize MediaPlayer for background sound
        initializeMediaPlayer()
        Log.d("SelectModesActivity", "onCreate: MediaPlayer initialized")

        // Set up button listeners with music stop
        findViewById<View>(R.id.cricketMode).setOnClickListener {
            stopMusic()
            startActivity(Intent(this, FlappyBallActivity::class.java))
            Log.d("SelectModesActivity", "Starting FlappyBallActivity")
        }
        findViewById<View>(R.id.ghostMode).setOnClickListener {
            stopMusic()
            startActivity(Intent(this, GhostGameActivity::class.java))
            Log.d("SelectModesActivity", "Starting GhostGameActivity")
        }
        findViewById<View>(R.id.islandMode).setOnClickListener {
            stopMusic()
            startActivity(Intent(this, IslandGameActivity::class.java))
            Log.d("SelectModesActivity", "Starting IslandGameActivity")
        }
        findViewById<View>(R.id.spaceMode).setOnClickListener {
            stopMusic()
            startActivity(Intent(this, SpaceGameActivity::class.java))
            Log.d("SelectModesActivity", "Starting SpaceGameActivity")
        }
    }

    private fun initializeMediaPlayer() {
        if (mediaPlayer == null) {
            try {
                mediaPlayer = MediaPlayer.create(this, R.raw.bgmusic)
                mediaPlayer?.isLooping = true // Loop the sound
                mediaPlayer?.start() // Start playing
                Log.d("SelectModesActivity", "MediaPlayer initialized and started")
            } catch (e: Exception) {
                android.widget.Toast.makeText(this, "Error loading background music", android.widget.Toast.LENGTH_SHORT).show()
                Log.e("SelectModesActivity", "Error initializing MediaPlayer", e)
            }
        }
    }

    private fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        Log.d("SelectModesActivity", "MediaPlayer stopped and released")
    }

    override fun onPause() {
        super.onPause()
        // Pause background music if initialized
        mediaPlayer?.pause()
        Log.d("SelectModesActivity", "onPause: MediaPlayer paused")
    }

    override fun onResume() {
        super.onResume()
        // Initialize and resume background music if not initialized
        initializeMediaPlayer()
        Log.d("SelectModesActivity", "onResume: MediaPlayer resumed or reinitialized")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up MediaPlayer
        stopMusic()
        Log.d("SelectModesActivity", "onDestroy: MediaPlayer cleaned up")
    }
}