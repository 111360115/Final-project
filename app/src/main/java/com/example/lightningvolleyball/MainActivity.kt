package com.example.lightningvolleyball

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle

import android.view.SurfaceHolder
import android.view.SurfaceView


import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

import kotlin.math.sqrt
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView
    private var running = true
    private var isTwoPlayerMode = false // 是否為雙人模式

    private lateinit var player1Image: Bitmap
    private lateinit var player2Image: Bitmap
    private lateinit var ballImage: Bitmap

    // 玩家狀態
    private var player1X = 200f
    private var player1Y = 0f
    private var player2X = 800f
    private var player2Y = 0f





    private var player1Score = 0 // 玩家1的分數
    private var player2Score = 0 // 玩家2的分數
    private lateinit var scoreSound: MediaPlayer
    private lateinit var player1WinSound: MediaPlayer
    private lateinit var player2WinSound: MediaPlayer

    // 球狀態
    private var ballX = 0f
    private var ballY = 0f
    private var ballVelocityX = 0f
    private var ballVelocityY = 0f

    private lateinit var scoreTextView: TextView
    private var isAiServing = false // 用來標記當前是否是AI的發球權
    private var isPlayer1Serving = false

    private var gameTimeInSeconds = 90 // 遊戲總時間為90秒（1分半）
    private var gameTimer: Job? = null // 用於停止計時的Job
    private lateinit var timerTextView: TextView // 用於顯示倒計時的TextView

    enum class AIDifficulty {
        EASY, MEDIUM, HARD
    }

    private var aiDifficulty: AIDifficulty = AIDifficulty.MEDIUM  // 預設難度為中等

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化計時器TextView
        timerTextView = findViewById(R.id.timerTextView)

        // 加載玩家和球的圖片
        player1Image = BitmapFactory.decodeResource(resources, R.drawable.fire)
        player2Image = BitmapFactory.decodeResource(resources, R.drawable.gt)
        ballImage = BitmapFactory.decodeResource(resources, R.drawable.ball)

        // 獲取 SurfaceView 引用
        surfaceView = findViewById(R.id.surfaceView)

        // 獲取 TextView 引用，用於顯示分數
        scoreTextView = findViewById(R.id.scoreTextView)

        // 顯示選擇遊戲模式的對話框
        showGameModeSelection()

        scoreSound = MediaPlayer.create(this, R.raw.score_sound)
        player1WinSound = MediaPlayer.create(this, R.raw.player1_win)
        player2WinSound = MediaPlayer.create(this, R.raw.player2_win)

        val surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                // 遊戲開始時顯示開始設置對話框
                showStartSetupDialog()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                running = false
            }
        })
    }

    // 顯示AI難度選擇對話框
    private fun showDifficultySelection() {
        // 使用 LayoutInflater 載入自定義布局
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_ai_difficulty, null)

        // 建立 AlertDialog 並設置自定義視圖
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // 初始化自定義布局中的按鈕
        val btnEasy = dialogView.findViewById<Button>(R.id.btn_easy)
        val btnMedium = dialogView.findViewById<Button>(R.id.btn_medium)
        val btnHard = dialogView.findViewById<Button>(R.id.btn_hard)

        // 設置按鈕的點擊事件
        btnEasy.setOnClickListener {
            aiDifficulty = AIDifficulty.EASY
            startGame()
            startCountdown()
            dialog.dismiss()
        }

        btnMedium.setOnClickListener {
            aiDifficulty = AIDifficulty.MEDIUM
            startGame()
            startCountdown()
            dialog.dismiss()
        }

        btnHard.setOnClickListener {
            aiDifficulty = AIDifficulty.HARD
            startGame()
            startCountdown()
            dialog.dismiss()
        }

        // 顯示 Dialog
        dialog.show()
    }

    // 顯示遊戲模式選擇對話框
    private fun showGameModeSelection() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("選擇遊戲模式")
        builder.setMessage("請選擇單人模式或雙人模式")
        builder.setPositiveButton("單人模式") { _, _ ->
            isTwoPlayerMode = false // 單人模式
            startGame()
            showDifficultySelection() // 顯示難度選擇
        }
        builder.setNegativeButton("雙人模式") { _, _ ->
            isTwoPlayerMode = true // 雙人模式
            startGame()
            // 啟動倒計時
            startCountdown()
        }
        builder.setCancelable(false)

        if (!isFinishing) {
            builder.show()
        }
    }

    // 顯示遊戲設置對話框
    private fun showStartSetupDialog() {
        val builder = AlertDialog.Builder(this)

        builder.setMessage("請調整遊戲設定。")
        builder.setPositiveButton("開始設定") { _, _ ->
            // 禁用控件，防止遊戲開始前用戶操作
            disableControls()

            // 根據模式設置發球方
            if (isTwoPlayerMode) {
                // 雙人模式隨機選擇發球方
                val isPlayer1Start = (0..1).random() == 0
                setBallPosition(isPlayer1Start)
            } else {
                // 單人模式由玩家1發球
                setBallPosition(true)  // true表示玩家1發球
            }

            // 遊戲開始
            startGame()
        }
        builder.setCancelable(false)
        builder.show()
    }














    private fun increaseScoreAndResetBall() {
        // 判斷球掉落的場地並更新分數
        if (ballX < surfaceView.width / 2) {
            // 如果球在左邊，玩家2得分
            player2Score++
            isPlayer1Serving = false
        } else {
            // 如果球在右邊，玩家1得分
            player1Score++
            isPlayer1Serving = true
        }

        // 播放得分音效
        scoreSound.start()

        // 更新分數顯示
        updateScore()

        // 判斷是否有玩家勝利
        if (player1Score == 5) {
            player1WinSound.start()
            showWinner("Player 1 ")
            return // 停止遊戲
        } else if (player2Score == 5) {
            player2WinSound.start()
            showWinner("Player 2 ")
            return // 停止遊戲
        }

        isAiServing = true

        // 得分後，繼續由得分方發球
        resetBall()

    }

    // 更新分數顯示
    private fun updateScore() {
        runOnUiThread {
            // 更新 TextView 中的分數顯示
            scoreTextView.text = "Score: Player 1: $player1Score  Player 2: $player2Score"
        }
    }

    // 顯示勝利者
    fun showWinner(winner: String) {
        // 禁用所有控件，確保無法繼續遊戲
        disableControls()

        // 創建並顯示對話框
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Game Over")
            builder.setMessage("$winner wins!") // 顯示誰贏了遊戲
            builder.setPositiveButton("重新開始") { _, _ ->
                // 重新開始遊戲
                restartGame()
            }
            builder.setNegativeButton("退出遊戲") { _, _ ->
                // 退出遊戲
                finish() // 結束遊戲並返回
            }
            builder.setCancelable(false)
            builder.show()
        }
    }



    // 開始倒計時
    private fun startCountdown() {
        // 使用 Coroutine 來更新時間
        gameTimer = CoroutineScope(Dispatchers.Main).launch {
            while (gameTimeInSeconds > 0) {
                delay(1000)  // 每秒更新一次
                gameTimeInSeconds--

                // 更新倒計時顯示
                updateTimerDisplay()
            }

            // 時間到時的處理
            onGameOver()
        }
    }

    // 遊戲結束處理
    private fun onGameOver() {
        // 可以在這裡處理時間到後的邏輯，比如結束遊戲、顯示提示等
        // 比如顯示遊戲結束對話框：

        if(player1Score>player2Score)
            showWinner("Time's Up! Leftside ")
        else
            showWinner("Time's Up! Rightside ")
    }

    // 更新計時器顯示
    private fun updateTimerDisplay() {
        val minutes = gameTimeInSeconds / 60
        val seconds = gameTimeInSeconds % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)

        timerTextView.text = "Time: $timeText"
    }

    // 處理玩家與球的碰撞
    private fun handlePlayerCollision(playerX: Float, playerY: Float, playerDirectionX: Int, ballX: Float, ballY: Float, playerJumping: Boolean) {
        ballVelocityX = when {
            playerDirectionX > 0 -> {
                if (ballX < playerX) 15f
                else 10f
            }
            playerDirectionX < 0 -> {
                if (ballX > playerX) -15f
                else -10f
            }
            ballX < playerX -> -10f
            else -> 10f
        }

        if (playerJumping) {
            ballVelocityY = -20f // 玩家跳躍時球應該反彈
        }

        // 如果玩家靜止且球從上方掉下來
        if (playerDirectionX == 0 && ballY > playerY - 50f && ballY < playerY) {
            ballVelocityY = -15f // 使球向上彈跳
        }
    }

    // 碰撞檢查
    private fun checkCollision(x1: Float, y1: Float, r1: Float, x2: Float, y2: Float, r2: Float): Boolean {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt((dx * dx + dy * dy).toDouble()) <= (r1 + r2)
    }

    // 繪製遊戲畫面
    private fun drawGame() {
        val canvas: Canvas? = surfaceView.holder.lockCanvas()
        if (canvas != null) {
            try {
                canvas.drawColor(Color.WHITE)  // 設置背景色
                val paint = Paint()

                // 繪製玩家1的圖片
                canvas.drawBitmap(player1Image, player1X - player1Image.width / 2, player1Y - player1Image.height / 2, paint)

                // 繪製玩家2的圖片
                canvas.drawBitmap(player2Image, player2X - player2Image.width / 2, player2Y - player2Image.height / 2, paint)

                // 繪製球的圖片
                canvas.drawBitmap(ballImage, ballX - ballImage.width / 2, ballY - ballImage.height / 2, paint)

                // 繪製網子
                paint.color = Color.BLACK
                canvas.drawRect(
                    surfaceView.width / 2f - 10,
                    surfaceView.height * 4 / 5f,
                    surfaceView.width / 2f + 10,
                    surfaceView.height.toFloat(),
                    paint
                )
            } finally {
                surfaceView.holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    // 釋放資源
    override fun onDestroy() {
        super.onDestroy()
        scoreSound.release() // 釋放資源
        player1WinSound.release()  // 釋放玩家1勝利音效資源
        player2WinSound.release()  // 釋放玩家2勝利音效資源
    }
}