package com.kbgaming.flappyball

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.Typeface
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlin.math.abs
import kotlin.random.Random

class GameView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    // Bird properties
    private var birdY = 500f
    private val birdRadius = 25f
    private var birdVelocity = 0f
    private val gravity = 2f
    private lateinit var birdBitmap: Bitmap

    // Obstacle properties
    private val obstacleWidth = 100f
    private val obstacleGap = 400f
    private var obstacleX = 800f
    private var obstacleSpeed = 17f
    private var upperObstacleHeight = 250f
    private var lowerObstacleHeight = 150f
    private lateinit var obstacleBitmap: Bitmap

    // Background
    private lateinit var backgroundBitmap: Bitmap

    // Game properties
    var isGameOver = false
    var isGameStarted = false
    var isGamePaused = false
    private val paint = android.graphics.Paint()
    private val handler = Handler(Looper.getMainLooper())
    private var score = 0
    private var highScore = 0
    private var soundPool: SoundPool? = null
    private var coinSoundId: Int = 0
    private var hasPassedObstacle = false
    private lateinit var customTypeface: Typeface

    // Reference to the ImageView for pause/play button
    private var pausePlayImageView: ImageView? = null

    // Status bar height for score text positioning
    private val statusBarHeight: Float by lazy { getStatusBarHeight().toFloat() }

    // SharedPreferences for high score
    private val prefs = context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)

    init {
        // Load bitmaps from resources
        try {
            birdBitmap = BitmapFactory.decodeResource(resources, R.drawable.ball)
            obstacleBitmap = BitmapFactory.decodeResource(resources, R.drawable.bat)
            backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.background)
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading images", Toast.LENGTH_SHORT).show()
            Log.e("GameView", "Error loading images", e)
        }

        // Initialize SoundPool for coin sound
        try {
            soundPool = SoundPool.Builder().setMaxStreams(1).build()
            coinSoundId = soundPool?.load(context, R.raw.coin, 1) ?: 0
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading coin sound", Toast.LENGTH_SHORT).show()
            Log.e("GameView", "Error loading coin sound", e)
        }

        // Load custom font
        try {
            customTypeface = Typeface.createFromAsset(context.assets, "fonts/orbitron_bold.ttf")
        } catch (e: Exception) {
            Log.e("GameView", "Error loading font", e)
            customTypeface = Typeface.DEFAULT_BOLD // Fallback
        }

        // Load high score from SharedPreferences
        highScore = prefs.getInt("highScore", 0)
        Log.d("GameView", "Loaded high score: $highScore")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Initialize pausePlayImageView when view is attached
        pausePlayImageView = (parent as? ViewGroup)?.findViewById<ImageView>(R.id.rightSideImage)
        Log.d("GameView", "onAttachedToWindow: pausePlayImageView is ${if (pausePlayImageView != null) "found" else "null"}")
        pausePlayImageView?.let {
            it.isClickable = true
            it.isFocusable = true
            it.setOnClickListener {
                Log.d("GameView", "Pause/Play ImageView clicked")
                togglePause()
            }
            // Set initial image to play since game is not started
            try {
                it.setImageResource(R.drawable.play)
                Log.d("GameView", "Initial play image set")
            } catch (e: Exception) {
                Log.e("GameView", "Error setting initial play image", e)
                Toast.makeText(context, "Error setting initial play image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Method to set ImageView from activity if needed
    fun setPausePlayImageView(imageView: ImageView) {
        pausePlayImageView = imageView
        pausePlayImageView?.let {
            it.isClickable = true
            it.isFocusable = true
            it.setOnClickListener {
                Log.d("GameView", "Pause/Play ImageView clicked (set manually)")
                togglePause()
            }
            // Set initial image
            try {
                it.setImageResource(if (isGameStarted && !isGamePaused) R.drawable.pause else R.drawable.play)
                Log.d("GameView", "ImageView set manually, image: ${if (isGameStarted && !isGamePaused) "pause" else "play"}")
            } catch (e: Exception) {
                Log.e("GameView", "Error setting image in setPausePlayImageView", e)
                Toast.makeText(context, "Error setting image", Toast.LENGTH_SHORT).show()
            }
        }
        Log.d("GameView", "PausePlayImageView set manually: ${if (pausePlayImageView != null) "found" else "null"}")
    }

    // Get status bar height in pixels
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        Log.d("GameView", "Status bar height: $result pixels")
        return result
    }

    // Runnable for game loop
    val runnable = Runnable {
        if (!isGameOver && isGameStarted && !isGamePaused) {
            Log.d("GameView", "Game loop running, score: $score, birdY: $birdY")
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        if (::backgroundBitmap.isInitialized) {
            val bgDest = RectF(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawBitmap(backgroundBitmap, null, bgDest, paint)
        } else {
            canvas.drawColor(android.graphics.Color.CYAN) // Fallback
        }

        // Draw bird
        if (::birdBitmap.isInitialized) {
            val birdSize = birdRadius * 2
            val birdRect = RectF(
                200f - birdRadius,
                birdY - birdRadius,
                200f + birdRadius,
                birdY + birdRadius
            )
            canvas.drawBitmap(birdBitmap, null, birdRect, paint)
        } else {
            paint.color = android.graphics.Color.YELLOW
            canvas.drawCircle(200f, birdY, birdRadius, paint)
        }

        // Draw upper obstacle
        if (::obstacleBitmap.isInitialized) {
            val upperRect = RectF(
                obstacleX,
                0f,
                obstacleX + obstacleWidth,
                upperObstacleHeight
            )
            canvas.drawBitmap(obstacleBitmap, null, upperRect, paint)
        } else {
            paint.color = android.graphics.Color.GREEN
            canvas.drawRect(obstacleX, 0f, obstacleX + obstacleWidth, upperObstacleHeight, paint)
        }

        // Draw lower obstacle
        val lowerObstacleTop = upperObstacleHeight + obstacleGap
        if (::obstacleBitmap.isInitialized) {
            val lowerRect = RectF(
                obstacleX,
                lowerObstacleTop,
                obstacleX + obstacleWidth,
                height.toFloat()
            )
            canvas.drawBitmap(obstacleBitmap, null, lowerRect, paint)
        } else {
            paint.color = android.graphics.Color.GREEN
            canvas.drawRect(obstacleX, lowerObstacleTop, obstacleX + obstacleWidth, height.toFloat(), paint)
        }

        // Set custom font and color
        paint.typeface = customTypeface
        paint.color = android.graphics.Color.parseColor("#320A6B") // Bright blue

        if (!isGameStarted) {
            // Draw "Double tap to start" prompt
            paint.textSize = 60f
            paint.textAlign = android.graphics.Paint.Align.CENTER
            canvas.drawText("Double tap to start", width / 2f, height / 2f, paint)
            paint.textAlign = android.graphics.Paint.Align.LEFT
        } else if (isGamePaused) {
            // Draw "Game Paused" prompt
            paint.textSize = 60f
            paint.textAlign = android.graphics.Paint.Align.CENTER
            canvas.drawText("Game Paused", width / 2f, height / 2f, paint)
            paint.textAlign = android.graphics.Paint.Align.LEFT
        } else if (!isGameOver) {
            // Update bird position
            birdY += birdVelocity
            birdVelocity += gravity
            Log.d("GameView", "Updating birdY: $birdY, velocity: $birdVelocity")

            // Check if ball is in the middle of the obstacle gap
            val ballX = 200f
            val tolerance = 50f
            val midpoint = obstacleX + obstacleWidth / 2
            if (!hasPassedObstacle && abs(ballX - midpoint) < tolerance) {
                hasPassedObstacle = true
                playCoinSound()
                Log.d("GameView", "Coin sound played at ballX: $ballX, midpoint: $midpoint")
            }

            // Move obstacle
            obstacleX -= obstacleSpeed

            // Reset obstacle and update score/speed
            if (obstacleX + obstacleWidth < 0) {
                obstacleX = width.toFloat()
                generateRandomObstacleHeights()
                score++
                // Update high score if current score exceeds it
                if (score > highScore) {
                    highScore = score
                    prefs.edit().putInt("highScore", highScore).apply()
                    Log.d("GameView", "New high score: $highScore")
                }
                setObstacleSpeed()
                hasPassedObstacle = false
                Log.d("GameView", "Obstacle reset, score: $score")
            }

            // Check for collisions
            if (checkCollision() || birdY - birdRadius < 0 || birdY + birdRadius > height) {
                isGameOver = true
                // Update high score on game over
                if (score > highScore) {
                    highScore = score
                    prefs.edit().putInt("highScore", highScore).apply()
                    Log.d("GameView", "Game over, new high score: $highScore")
                }
                showGameOverDialog()
                Log.d("GameView", "Game over triggered")
            }
        }

        // Display scores if game started
        if (isGameStarted) {
            paint.textSize = 35f
            val highScoreY = statusBarHeight + 50f
            val scoreY = statusBarHeight + 100f // Below high score
            canvas.drawText("Highest Score: $highScore", 50f, highScoreY, paint)
            canvas.drawText("Score: $score", 50f, scoreY, paint)
            Log.d("GameView", "Drawing high score at y: $highScoreY, score at y: $scoreY")
        }

        if (!isGameOver && isGameStarted && !isGamePaused) {
            handler.postDelayed(runnable, 25)
        }
    }

    fun startGame() {
        if (!isGameStarted) {
            isGameStarted = true
            isGamePaused = false
            handler.post(runnable)
            // Set pause image when game starts
            pausePlayImageView?.let {
                try {
                    it.setImageResource(R.drawable.pause)
                    Log.d("GameView", "Game started, set pause image")
                } catch (e: Exception) {
                    Log.e("GameView", "Error setting pause image in startGame", e)
                    Toast.makeText(context, "Error setting pause image", Toast.LENGTH_SHORT).show()
                }
            } ?: Log.e("GameView", "pausePlayImageView is null in startGame")
        }
    }

    fun togglePause() {
        if (isGameStarted && !isGameOver) {
            isGamePaused = !isGamePaused
            pausePlayImageView?.let {
                try {
                    if (isGamePaused) {
                        handler.removeCallbacks(runnable)
                        it.setImageResource(R.drawable.play)
                        Log.d("GameView", "Game paused, set play image")
                    } else {
                        handler.post(runnable)
                        it.setImageResource(R.drawable.pause)
                        Log.d("GameView", "Game resumed, set pause image")
                    }
                } catch (e: Exception) {
                    Log.e("GameView", "Error setting image in togglePause", e)
                    Toast.makeText(context, "Error setting image in togglePause", Toast.LENGTH_SHORT).show()
                }
            } ?: Log.e("GameView", "pausePlayImageView is null in togglePause")
            invalidate()
        } else {
            Log.d("GameView", "togglePause ignored, isGameStarted: $isGameStarted, isGameOver: $isGameOver")
        }
    }

    private fun playCoinSound() {
        try {
            soundPool?.play(coinSoundId, 1f, 1f, 1, 0, 1f)
        } catch (e: Exception) {
            Toast.makeText(context, "Error playing coin sound", Toast.LENGTH_SHORT).show()
            Log.e("GameView", "Error playing coin sound", e)
        }
    }

    private fun checkCollision(): Boolean {
        val offset = 50f
        val ballX = 200f
        val ballRadius = 30f

        val upperRectLeft = obstacleX + offset
        val upperRectRight = obstacleX + obstacleWidth - offset
        val upperRectBottom = upperObstacleHeight

        val lowerRectLeft = obstacleX + offset
        val lowerRectRight = obstacleX + obstacleWidth - offset
        val lowerRectTop = upperObstacleHeight + obstacleGap

        val closestXUpper = ballX.coerceIn(upperRectLeft, upperRectRight)
        val closestYUpper = birdY.coerceIn(0f, upperRectBottom)
        val closestXLower = ballX.coerceIn(lowerRectLeft, lowerRectRight)
        val closestYLower = birdY.coerceIn(lowerRectTop, height.toFloat())

        val distXUpper = ballX - closestXUpper
        val distYUpper = birdY - closestYUpper
        val distXLower = ballX - closestXLower
        val distYLower = birdY - closestYLower

        val upperCollision = (distXUpper * distXUpper + distYUpper * distYUpper) < (ballRadius * ballRadius)
        val lowerCollision = (distXLower * distXLower + distYLower * distYLower) < (ballRadius * ballRadius)

        return upperCollision || lowerCollision
    }

    private fun setObstacleSpeed() {
        obstacleSpeed = when {
            score > 34 -> 30f
            score > 20 -> 27f
            score > 10 -> 22f
            else -> 17f
        }
    }

    private fun generateRandomObstacleHeights() {
        val maxUpperHeight = (height / 2) - obstacleGap / 2
        val minUpperHeight = 200
        upperObstacleHeight = Random.nextInt(minUpperHeight, maxUpperHeight.toInt()).toFloat()
        val lowerMinHeight = 200
        lowerObstacleHeight = height - (upperObstacleHeight + obstacleGap).coerceAtLeast(lowerMinHeight.toFloat())
    }

    fun moveUp() {
        if (!isGameOver && isGameStarted && !isGamePaused) {
            birdVelocity = -19f
            Log.d("GameView", "moveUp called, birdVelocity: $birdVelocity")
        } else {
            Log.d("GameView", "moveUp ignored, isGameStarted: $isGameStarted, isGameOver: $isGameOver, isGamePaused: $isGamePaused")
        }
    }

    fun moveDown() {
        if (!isGameOver && isGameStarted && !isGamePaused) {
            birdVelocity += 10f
            Log.d("GameView", "moveDown called, birdVelocity: $birdVelocity")
        } else {
            Log.d("GameView", "moveDown ignored, isGameStarted: $isGameStarted, isGameOver: $isGameOver, isGamePaused: $isGamePaused")
        }
    }

    fun resetGame() {
        birdY = 500f
        birdVelocity = 0f
        obstacleX = width.toFloat()
        generateRandomObstacleHeights()
        score = 0
        obstacleSpeed = 17f
        isGameOver = false
        hasPassedObstacle = false
        isGameStarted = false
        isGamePaused = false
        // Reload high score to ensure it's up-to-date
        highScore = prefs.getInt("highScore", 0)
        // Set play image when game is reset
        pausePlayImageView?.let {
            try {
                it.setImageResource(R.drawable.play)
                Log.d("GameView", "Game reset, high score: $highScore, set play image")
            } catch (e: Exception) {
                Log.e("GameView", "Error setting play image in resetGame", e)
                Toast.makeText(context, "Error setting play image", Toast.LENGTH_SHORT).show()
            }
        } ?: Log.e("GameView", "pausePlayImageView is null in resetGame")
        invalidate()
        Log.d("GameView", "Game reset, high score: $highScore")
    }

    private fun showGameOverDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.game_over_dialog, null)
        val scoreTextView = dialogView.findViewById<TextView>(R.id.score_text)
        val imageView = dialogView.findViewById<ImageView>(R.id.game_over_image)
        val restartButton = dialogView.findViewById<Button>(R.id.restartButton)
        val exitButton = dialogView.findViewById<Button>(R.id.exitButton)

        scoreTextView.text = "Your score is: $score"

        val imageResId = if (score > 20) R.drawable.img1 else R.drawable.img2
        try {
            imageView.setImageResource(imageResId)
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading dialog image", Toast.LENGTH_SHORT).show()
            Log.e("GameView", "Error loading dialog image", e)
        }

        val animation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
        dialogView.startAnimation(animation)

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogView)
        builder.setCancelable(false)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        restartButton.setOnClickListener {
            resetGame()
            dialog.dismiss()
        }
        exitButton.setOnClickListener {
            (context as? FlappyBallActivity)?.finish()
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(runnable)
        soundPool?.release()
        soundPool = null
        pausePlayImageView = null
        Log.d("GameView", "View detached, resources cleaned up")
    }
}