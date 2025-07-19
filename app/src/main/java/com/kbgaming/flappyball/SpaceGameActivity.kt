package com.kbgaming.flappyball

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat

class SpaceGameActivity : AppCompatActivity() {

    private lateinit var gameView: SpaceGame
    private lateinit var gestureDetector: GestureDetectorCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_space_game)

        gameView = findViewById(R.id.spaceGameView)

        // Set up click listener for pause/resume image
        findViewById<ImageView>(R.id.rightSideImage).setOnClickListener {
            gameView.togglePause()
            Log.d("SpaceGameActivity", "Pause/Resume image clicked")
        }

        // Initialize GestureDetector for double-tap detection
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (!gameView.isGameStarted) {
                    Log.d("SpaceGameActivity", "Double-tap detected, starting game")
                    gameView.startGame()
                    return true
                }
                return false
            }
        })

        // Set status bar color and icons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = ContextCompat.getColor(this, R.color.spacemode)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.spacemode)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Handle single-tap for moveUp
        if (event.action == MotionEvent.ACTION_DOWN && gameView.isGameStarted && !gameView.isGameOver && !gameView.isGamePaused) {
            Log.d("SpaceGameActivity", "Single-tap detected, calling moveUp")
            gameView.moveUp()
            return true
        }
        // Pass all events to GestureDetector for double-tap detection
        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    override fun onPause() {
        super.onPause()
        Log.d("SpaceGameActivity", "Pausing game")
        gameView.isGamePaused = true
        gameView.handler.removeCallbacks(gameView.runnable)
    }

    override fun onResume() {
        super.onResume()
        if (gameView.isGameStarted && !gameView.isGameOver && !gameView.isGamePaused) {
            Log.d("SpaceGameActivity", "Resuming game")
            gameView.handler.post(gameView.runnable)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SpaceGameActivity", "Destroying activity")
        gameView.handler.removeCallbacks(gameView.runnable)
    }
}