import android.content.Context
import android.graphics.Canvas
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private lateinit var gameThread: GameThread
    private val ball = Ball(500f, 500f, 0f, 0f)
    private val player1 = Player(200f, 1700f, android.graphics.Color.YELLOW)
    private val player2 = Player(800f, 1700f, android.graphics.Color.RED)

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        gameThread = GameThread(this)
        gameThread.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        gameThread.stopThread()
    }

    fun updateGame() {
        val screenWidth = width.toFloat()
        val screenHeight = height.toFloat()
        val groundY = screenHeight - 50f

        // 更新玩家和球逻辑
        player1.update(2f, groundY)
        player2.update(2f, groundY)
        ball.update()
        ball.checkBoundaryCollision(screenWidth, screenHeight)

        // 检测球与玩家碰撞
        if (ball.checkPlayerCollision(player1.x, player1.y, Player.RADIUS)) {
            ball.vx = 15f
            ball.vy = -20f
        }
        if (ball.checkPlayerCollision(player2.x, player2.y, Player.RADIUS)) {
            ball.vx = -15f
            ball.vy = -20f
        }
    }

    fun renderGame() {
        val canvas = holder.lockCanvas()
        if (canvas != null) {
            try {
                canvas.drawColor(android.graphics.Color.WHITE)
                ball.draw(canvas)
                player1.draw(canvas)
                player2.draw(canvas)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }
}
