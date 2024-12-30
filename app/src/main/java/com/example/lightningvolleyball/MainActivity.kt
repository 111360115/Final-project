package com.example.lightningvolleyball

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random
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
    private var playerSpeed = 65f

    private var player1VelocityY = 0f
    private var player2VelocityY = 0f
    private var gravity = 1f
    private var jumpVelocity = -25f
    private var player1Jumping = false
    private var player2Jumping = false

    private var player1DirectionX = 0 // -1 為左移, 1 為右移, 0 為不移動
    private var player2DirectionX = 0 // -1 為左移, 1 為右移, 0 為不移動

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
    private var ballIsActive = false // 球是否開始移動
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
        val builder = AlertDialog.Builder(this)
        builder.setTitle("選擇AI難度")
        builder.setMessage("請選擇AI的難度")

        builder.setPositiveButton("簡單") { _, _ ->
            aiDifficulty = AIDifficulty.EASY
            startGame()
            // 啟動倒計時
            startCountdown()
        }
        builder.setNeutralButton("中等") { _, _ ->
            aiDifficulty = AIDifficulty.MEDIUM
            startGame()
            // 啟動倒計時
            startCountdown()
        }
        builder.setNegativeButton("困難") { _, _ ->
            aiDifficulty = AIDifficulty.HARD
            startGame()
            // 啟動倒計時
            startCountdown()
        }

        builder.setCancelable(false)
        builder.show()
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

    // 開始遊戲
    private fun startGame() {
        val groundY = surfaceView.height - 70f
        player1Y = groundY
        player2Y = groundY

        enableControls()

        // 啟動遊戲循環
        thread {
            gameLoop()
        }
    }

    // 禁用控件
    private fun disableControls() {
        runOnUiThread {
            val startGameButton = findViewById<Button>(R.id.startGameButton)
            startGameButton?.isEnabled = false // 禁用按鈕
            startGameButton?.visibility = View.GONE // 隱藏按鈕
            findViewById<SurfaceView>(R.id.surfaceView)?.setOnTouchListener(null) // 禁用觸摸

            // 禁用玩家的移動和跳躍
            player1DirectionX = 0
            player2DirectionX = 0
        }
    }

    // 啟用控件
    private fun enableControls() {
        val startGameButton = findViewById<Button>(R.id.startGameButton)
        startGameButton?.isEnabled = true // 啟用按鈕
        startGameButton?.visibility = View.GONE // 確保按鈕不再顯示
        findViewById<SurfaceView>(R.id.surfaceView)?.setOnTouchListener { v, event ->
            // 處理觸摸事件
            true
        }
    }

    // 設置球的位置
    private fun setBallPosition(isPlayer1Start: Boolean) {
        ballIsActive = false
        val groundY = surfaceView.height - 70f
        val jumpHitHeight = groundY - 150f
        if (isPlayer1Start) {
            ballX = 300f
            ballY = jumpHitHeight
        } else {
            ballX = surfaceView.width - 300f
            ballY = jumpHitHeight
        }
        ballVelocityX = 0f
        ballVelocityY = 0f
    }

    // 處理玩家1的按鍵事件
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val netX = surfaceView.width / 2f

        when (keyCode) {
            KeyEvent.KEYCODE_A -> {
                player1X = (player1X - playerSpeed).coerceAtLeast(0f)
                player1DirectionX = -1 // 左移
            }
            KeyEvent.KEYCODE_D -> {
                player1X = (player1X + playerSpeed).coerceAtMost(netX - 50f)
                player1DirectionX = 1 // 右移
            }
            KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_SPACE -> {
                if (!player1Jumping) {
                    player1Jumping = true
                    player1VelocityY = jumpVelocity
                }
            }
        }

        if (isTwoPlayerMode) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    player2X = (player2X - playerSpeed).coerceAtLeast(netX + 50f)
                    player2DirectionX = -1 // 左移
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    player2X = (player2X + playerSpeed).coerceAtMost(surfaceView.width - 50f)
                    player2DirectionX = 1 // 右移
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (!player2Jumping) {
                        player2Jumping = true
                        player2VelocityY = jumpVelocity
                    }
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    // 處理玩家1按鍵鬆開事件
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_D -> player1DirectionX = 0 // 停止水平移動
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> player2DirectionX = 0 // 停止水平移動
        }
        return super.onKeyUp(keyCode, event)
    }

    // 遊戲循環
    private fun gameLoop() {

        while (running) {
            Thread.sleep(4) // 每10毫秒更新
            updateGameState()

            // 檢查是否有玩家勝利
            if (player1Score == 5 || player2Score == 5) {
                break // 結束遊戲循環
            }

            drawGame()
        }
    }

    // 更新遊戲狀態
    private fun updateGameState() {
        val groundY = surfaceView.height - 70f // 地面高度
        val netX = surfaceView.width / 2f // 網子的 X 軸位置
        val netTopY = surfaceView.height * 4 / 5f // 網子的頂部 Y 軸位置
        val playerRadius = 50f // 玩家半徑
        val ballRadius = 30f // 球半徑
        val boundaryBuffer = 15f


        // 玩家1移動邏輯
        player1X += playerSpeed * player1DirectionX
        player1X = player1X.coerceIn(0f, netX - playerRadius - boundaryBuffer) // 增加緩衝區，限制玩家1的 X 軸移動範圍，不能讓玩家1的右邊緣越過網子的左邊界

        if (isTwoPlayerMode) {
            // 玩家2移動邏輯
            player2X += playerSpeed * player2DirectionX
            player2X = player2X.coerceIn(netX + playerRadius + boundaryBuffer, surfaceView.width - playerRadius - boundaryBuffer) // 增加緩衝區，限制玩家2的 X 軸移動範圍，不能讓玩家2的左邊緣越過網子的右邊界
        } else {
            // AI 控制邏輯（單人模式）

            if (ballIsActive || isAiServing) {
                val speedMultiplier = when (aiDifficulty) {
                    AIDifficulty.EASY -> 0.1f
                    AIDifficulty.MEDIUM -> 1f
                    AIDifficulty.HARD -> 5f
                }

                // 根據難度控制 AI 的移動速度
                if (ballX > player2X) {
                    player2X += (playerSpeed / 2) * speedMultiplier // AI 向右移動
                    player2DirectionX = 1
                } else if (ballX < player2X) {
                    player2X -= (playerSpeed / 2) * speedMultiplier // AI 向左移動
                    player2DirectionX = -1
                } else {
                    player2DirectionX = 0 // AI 停止移動
                }

                // AI 跳躍邏輯：AI 嘗試在球接近時跳躍
                val jumpTriggerDistance = when (aiDifficulty) {
                    AIDifficulty.EASY -> 300f // 簡單難度，AI會稍微遠離球
                    AIDifficulty.MEDIUM -> 100f // 中等難度，AI會較為靈敏
                    AIDifficulty.HARD -> 50f // 高難度，AI非常靈敏
                }

                if (!player2Jumping && ballY < player2Y && Math.abs(ballX - player2X) < jumpTriggerDistance) {
                    player2Jumping = true
                    player2VelocityY = jumpVelocity
                }
            }

            player2X = player2X.coerceIn(netX + playerRadius + boundaryBuffer, surfaceView.width - playerRadius - boundaryBuffer) // 限制玩家2的 X 軸移動範圍
        }

        // 玩家1跳躍邏輯
        if (player1Jumping) {
            player1Y += player1VelocityY
            player1VelocityY += gravity // 加上較小的重力效果
            if (player1Y >= groundY) {
                player1Y = groundY
                player1Jumping = false // 結束跳躍
            }
        }

        // 玩家2（或AI）跳躍邏輯
        if (player2Jumping) {
            player2Y += player2VelocityY
            player2VelocityY += gravity // 加上較小的重力效果
            if (player2Y >= groundY) {
                player2Y = groundY
                player2Jumping = false // 結束跳躍
            }
        }

        // 球邏輯
        if (ballIsActive) {
            ballX += ballVelocityX // 更新球的 X 軸位置
            ballY += ballVelocityY // 更新球的 Y 軸位置
            ballVelocityY += gravity/2 // 使用較小的重力效果來讓球下落更慢

            // 球碰撞左右邊界
            if (ballX <= 0 || ballX >= surfaceView.width) {
                ballVelocityX = -ballVelocityX // 水平反彈
            }

            // 球碰到地面
            if (ballY + ballRadius >= groundY+80f) {
                ballY = groundY
                ballVelocityY = 0f
                ballVelocityX = 0f
                ballIsActive = false // 停止球的運動

                // 增加對方分數並更新球的位置
                increaseScoreAndResetBall()
            }

            // 球碰到網子
            if (ballX + ballRadius >= netX - 20 && ballX - ballRadius <= netX + 20 && ballY + ballRadius >= netTopY) {
                // 球的邊緣觸碰到網子的邊緣
                if (ballVelocityX > 0) {
                    // 球從左向右飛，碰到網子右邊反彈
                    ballVelocityX = -ballVelocityX  // 反轉X軸速度，讓球反彈
                } else if (ballVelocityX < 0) {
                    // 球從右向左飛，碰到網子左邊反彈
                    ballVelocityX = -ballVelocityX // 反轉X軸速度，讓球反彈
                }

                // 反轉Y軸速度，確保球在垂直方向上反彈
                ballVelocityY = -ballVelocityY  // 反轉Y軸速度，讓球反彈

                // 可以稍微減慢水平方向速度，模擬摩擦力
                //ballVelocityX *= 0.9f // 可以稍微減慢水平方向的速度，模擬摩擦力

                ballIsActive = true  // 確保球保持活躍
            }

            // 玩家1與球的碰撞檢測
            if (checkCollision(player1X, player1Y, playerRadius, ballX, ballY, ballRadius)) {
                handlePlayerCollision(player1X, player1Y, player1DirectionX, ballX, ballY, player1Jumping)
            }

            // 玩家2（或AI）與球的碰撞檢測
            if (checkCollision(player2X, player2Y, playerRadius, ballX, ballY, ballRadius)) {
                handlePlayerCollision(player2X, player2Y, player2DirectionX, ballX, ballY, player2Jumping)
            }

        } else {
            // 玩家1觸發球運動
            if (checkCollision(player1X, player1Y, playerRadius, ballX, ballY, ballRadius)) {
                ballIsActive = true
                ballVelocityX = if (player1DirectionX != 0) 15f * player1DirectionX else 15f
                ballVelocityY = -20f
            }
            // 玩家2（或AI）觸發球運動
            if (checkCollision(player2X, player2Y, playerRadius, ballX, ballY, ballRadius)) {
                ballIsActive = true
                ballVelocityX = if (player2DirectionX != 0) 15f * player2DirectionX else -15f
                ballVelocityY = -15f
            }
        }

    }

    // 重置球的位置到對方的場地
    private fun resetBall() {
        val jumpHitHeight = surfaceView.height - 50f - 150f // 設置球的跳躍高度

        Thread.sleep(150)

        // 根據當前發球方來設置發球位置
        if (isPlayer1Serving) {
            // 如果是玩家1發球
            ballX = 300f  // 玩家1的發球位置
        } else {
            // 如果是玩家2發球
            ballX = surfaceView.width - 300f  // 玩家2的發球位置
        }

        ballY = jumpHitHeight  // 設置球的高度
        ballVelocityX = 0f  // 水平速度為0
        ballVelocityY = 0f  // 垂直速度為0
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

    // 重新開始遊戲的邏輯
    private fun restartGame() {
        // 重置分數
        player1Score = 0
        player2Score = 0

        // 停止並取消現有的倒計時計時器
        gameTimer?.cancel()  // 停止計時器
        gameTimer = null  // 清除計時器引用


        // 重置時間
        gameTimeInSeconds = 90
        updateTimerDisplay()

        // 重置球的位置
        resetBall()

        // 重新開始遊戲
        showGameModeSelection()
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
